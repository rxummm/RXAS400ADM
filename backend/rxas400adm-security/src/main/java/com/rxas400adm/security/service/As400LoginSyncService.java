package com.rxas400adm.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import com.rxas400adm.common.config.ProfileResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AS400 账号每日同步（追踪文档 2.2.5，参考旧项目 As400LoginSyncService）：
 * <ul>
 *   <li><b>清理失效账号</b>：对每个启用服务器上 login_source=AS400 的本地用户，查询 IBM i
 *       user profile；已不存在（userProfile 为空）→ 删除本地用户（连带角色关系）</li>
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
        int cleaned = 0;
        int converged = 0;
        for (IbmiSystem system : systems) {
            try {
                AS400Client client = clientProvider.forServer(system.getId());
                List<SysUser> users = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getLoginSource, "AS400")
                                .eq(SysUser::getAs400ServerId, system.getId()));
                if (users.isEmpty()) {
                    continue;
                }
                // 先探测全部账号，再按命中情况区分「整机故障」与「个别失效」
                List<SysUser> missing = new ArrayList<>();
                for (SysUser user : users) {
                    com.rxas400adm.as400.model.UserProfileRow profile = client.userProfile(user.getUsername());
                    if (profile == null || profile.userName() == null || profile.userName().isBlank()) {
                        missing.add(user);
                    } else {
                        // 组角色收敛：按映射重算并覆盖
                        String group = profile.groupProfile() == null
                                ? "" : profile.groupProfile();
                        String roleCode = as400LoginService.loadGroupRoleMapping()
                                .getOrDefault(group, As400LoginService.DEFAULT_ROLE);
                        as400LoginService.applyRoles(user.getId(), List.of(roleCode));
                        // 角色收敛后失效权限缓存，下次请求即按新角色加载
                        permissionService.evict(user.getUsername());
                        converged++;
                    }
                }
                if (missing.isEmpty()) {
                    outageStreak.remove(system.getId());
                    continue;
                }
                boolean allMissing = missing.size() == users.size();
                if (allMissing) {
                    // M2：整机疑似故障/断连——全部 profile 查询为空，暂缓清理，连续 N 轮仍全空才按失效处理
                    int streak = outageStreak.merge(system.getId(), 1, Integer::sum);
                    log.warn("[AS400同步] 服务器 {} 全部 {} 个 AS400 账号 profile 查询为空（连续 {} 轮），疑似故障，暂缓清理",
                            system.getName(), users.size(), streak);
                    if (streak < OUTAGE_STREAK_LIMIT) {
                        continue;
                    }
                    log.error("[AS400同步] 服务器 {} 连续 {} 轮全空，仍按失效账号清理 {} 个",
                            system.getName(), streak, users.size());
                    outageStreak.remove(system.getId());
                } else {
                    // 部分缺失：个别账号确实在 IBM i 上已删除，正常清理
                    outageStreak.remove(system.getId());
                }
                for (SysUser user : missing) {
                    // 失效账号：IBM i 上已删除（整机故障场景下为连续 N 轮确认后）
                    userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, user.getId()));
                    userMapper.deleteById(user.getId());
                    // S1：删除后立即失效权限缓存，其 token 不再有权限
                    permissionService.evict(user.getUsername());
                    cleaned++;
                    log.info("[AS400同步] 清理失效账号 {} (server={})", user.getUsername(), system.getName());
                }
            } catch (Exception e) {
                log.warn("[AS400同步] 服务器 {} 处理失败，跳过: {}", system.getName(), e.getMessage());
            }
        }
        log.info("[AS400同步] 完成：清理 {}，收敛 {}", cleaned, converged);
    }
}