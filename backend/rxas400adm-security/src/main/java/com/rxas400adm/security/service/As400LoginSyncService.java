package com.rxas400adm.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.as400.model.UserProfileRow;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AS400 账号每日同步（追踪文档 2.2.5，参考旧项目 As400LoginSyncService）：
 * <ul>
 *   <li><b>清理失效账号</b>：对每个启用服务器上 login_source=AS400 的本地用户，批量查询 IBM i
 *       全库用户（P15：单条 QSYS2.USER_INFO SQL 替代逐账号 RPC）；未命中 → 删除本地用户（连带角色关系）</li>
 *   <li><b>组角色收敛</b>：按该用户当前组 profile 的映射结果整体覆盖本地角色
 *       （组中移除 → 自动失去对应角色）</li>
 * </ul>
 * 按服务器逐个处理，单服务器查询失败单独跳过；mock 模式（无真实 IBM i）跳过整个任务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class As400LoginSyncService implements IAs400LoginSyncService {

    private final IbmiSystemMapper systemMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final AS400ClientProvider clientProvider;
    private final IAs400LoginService as400LoginService;
    private final IPermissionService permissionService;

    private final ProfileResolver profileResolver;

    /** M2：整机疑似故障保护——某服务器全部 AS400 账号 profile 查询为空时，连续 N 轮才允许清理 */
    private static final int OUTAGE_STREAK_LIMIT = 2;
    private final ConcurrentMap<Long, Integer> outageStreak = new ConcurrentHashMap<>();

    /** 每天 02:00 执行 */
    @Scheduled(cron = "0 0 2 * * *")
    public void dailySync() {
        if (profileResolver.isMockMode()) {
            log.info("[AS400同步] mock 模式跳过每日同步任务");
            return;
        }
        List<IbmiSystem> systems = systemMapper.selectList(
                new LambdaQueryWrapper<IbmiSystem>().eq(IbmiSystem::getEnabled, true));
        SyncResult total = SyncResult.ZERO;
        for (IbmiSystem system : systems) {
            try {
                total = total.plus(syncServer(system));
            } catch (Exception e) {
                log.warn("[AS400同步] 服务器 {} 处理失败，跳过: {}", system.getName(), e.getMessage());
            }
        }
        log.info("[AS400同步] 完成：清理 {}，收敛 {}", total.cleaned(), total.converged());
    }

    /** 单服务器同步结果（中-5：dailySync 四职责拆分后的聚合返回值） */
    private record SyncResult(int cleaned, int converged) {
        static final SyncResult ZERO = new SyncResult(0, 0);

        SyncResult plus(SyncResult o) {
            return new SyncResult(cleaned + o.cleaned, converged + o.converged);
        }
    }

    /** 单服务器：探测全部账号 profile → 命中者组角色收敛；缺失者经熔断确认后清理 */
    private SyncResult syncServer(IbmiSystem system) {
        AS400Client client = clientProvider.forServer(system.getId());
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getLoginSource, "AS400")
                        .eq(SysUser::getAs400ServerId, system.getId()));
        if (users.isEmpty()) {
            return SyncResult.ZERO;
        }
        // P15 批量探测：一次拉全库 USER_INFO 构建 name→profile 映射，替代逐账号 RPC
        Map<String, UserProfileRow> profiles = loadAllProfiles(client);
        // 再按命中情况区分「整机故障」与「个别失效」
        List<SysUser> missing = new ArrayList<>();
        int converged = 0;
        for (SysUser user : users) {
            UserProfileRow profile = profiles.get(profileKey(user.getUsername()));
            if (isMissing(profile)) {
                missing.add(user);
            } else {
                convergeRoles(user, profile);
                converged++;
            }
        }
        if (missing.isEmpty()) {
            outageStreak.remove(system.getId());
            return new SyncResult(0, converged);
        }
        if (!confirmCleanup(system, users.size(), missing.size())) {
            return new SyncResult(0, converged);
        }
        return new SyncResult(cleanupMissing(system, missing), converged);
    }

    /**
     * CR-003 修复：分页查询 IBM i 全库用户（QSYS2.USER_INFO），避免 5000 上限导致用户误删。
     * 每批 FETCH FIRST 2000 ROWS ONLY，循环直到无更多数据，构建完整 name→profile 映射。
     * 查询失败直接抛出，由 dailySync 按服务器粒度跳过；空名行不入映射。
     */
    private Map<String, UserProfileRow> loadAllProfiles(AS400Client client) {
        Map<String, UserProfileRow> profiles = new HashMap<>();
        int batchSize = 2000;
        int offset = 0;
        boolean hasMore = true;
        while (hasMore) {
            // DB2 for i 支持 OFFSET/FETCH：跳过已读行，取下一批
            String baseSql;
            try {
                baseSql = SqlStatementRegistry.of("sync.user.list");
            } catch (IllegalStateException e) {
                baseSql = "SELECT AUTHORIZATION_NAME, GROUP_PROFILE_NAME FROM QSYS2.USER_INFO ORDER BY AUTHORIZATION_NAME";
            }
            String sql = baseSql + String.format(" OFFSET %d ROWS FETCH FIRST %d ROWS ONLY",
                    offset, batchSize);
            List<Map<String, Object>> rows = client.queryListCheckedBounded(sql, batchSize);
            if (rows.isEmpty()) {
                hasMore = false;
                break;
            }
            for (Map<String, Object> row : rows) {
                Object name = row.get("AUTHORIZATION_NAME");
                if (name == null || String.valueOf(name).isBlank()) {
                    continue;
                }
                String group = row.get("GROUP_PROFILE_NAME") == null ? null :
                        String.valueOf(row.get("GROUP_PROFILE_NAME"));
                profiles.put(profileKey(String.valueOf(name)),
                        new UserProfileRow(String.valueOf(name), group, null));
            }
            // 如果本批返回行数 < batchSize，说明已是最后一批
            hasMore = rows.size() >= batchSize;
            offset += rows.size();
        }
        log.info("[AS400同步] 加载全库用户 profile {} 条（{} 批次）", profiles.size(),
                (offset + batchSize - 1) / batchSize);
        return profiles;
    }

    /** P15：IBM i 用户名统一大写去空白作为映射键（AUTHORIZATION_NAME 大小写不敏感对齐） */
    private static String profileKey(String username) {
        return username == null ? "" : username.trim().toUpperCase();
    }

    /** profile 查询为空视为账号在 IBM i 上已不存在 */
    private static boolean isMissing(UserProfileRow profile) {
        return profile == null || profile.userName() == null || profile.userName().isBlank();
    }

    /** 组角色收敛：按映射重算并整体覆盖，随后失效权限缓存（下次请求即按新角色加载） */
    private void convergeRoles(SysUser user, UserProfileRow profile) {
        String group = profile.groupProfile() == null ? "" : profile.groupProfile();
        String roleCode = as400LoginService.loadGroupRoleMapping()
                .getOrDefault(group, As400LoginService.DEFAULT_ROLE);
        as400LoginService.applyRoles(user.getId(), List.of(roleCode));
        permissionService.evict(user.getUsername());
    }

    /** M2 整机故障熔断：部分缺失直接放行；全部缺失需连续 N 轮确认才放行清理 */
    private boolean confirmCleanup(IbmiSystem system, int totalUsers, int missingCount) {
        if (missingCount < totalUsers) {
            // 部分缺失：个别账号确实在 IBM i 上已删除，正常清理
            outageStreak.remove(system.getId());
            return true;
        }
        int streak = outageStreak.merge(system.getId(), 1, Integer::sum);
        log.warn("[AS400同步] 服务器 {} 全部 {} 个 AS400 账号 profile 查询为空（连续 {} 轮），疑似故障，暂缓清理",
                system.getName(), totalUsers, streak);
        if (streak < OUTAGE_STREAK_LIMIT) {
            return false;
        }
        log.error("[AS400同步] 服务器 {} 连续 {} 轮全空，仍按失效账号清理 {} 个",
                system.getName(), streak, totalUsers);
        outageStreak.remove(system.getId());
        return true;
    }

    /** 失效账号清理：删角色关系 + 删用户 + 失效权限缓存（其 token 不再有权限） */
    private int cleanupMissing(IbmiSystem system, List<SysUser> missing) {
        int cleaned = 0;
        for (SysUser user : missing) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, user.getId()));
            userMapper.deleteById(user.getId());
            permissionService.evict(user.getUsername());
            cleaned++;
            log.info("[AS400同步] 清理失效账号 {} (server={})", user.getUsername(), system.getName());
        }
        return cleaned;
    }
}
