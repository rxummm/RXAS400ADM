package com.rxas400adm.security.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import com.rxas400adm.system.service.IMenuService;
import com.rxas400adm.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限加载（2.5.1 增强）：Caffeine 缓存用户名 → 权限码列表（TTL 60s），\n * 显著减少 RBAC 打库。角色/状态变更后最多 60s 生效；\n * 登录成功时也刷新缓存，保证权限变化及时体现。\n * \n * 权限来源统一（2026-08-12）：\n * - rx_permission 权限码（rx_role_permission 绑定，含 ADMIN 全量）\n * - 菜单授权（rx_role_menu/rx_user_menu：menu_type 1/2/3 中带 perms 的菜单，参照旧项目）\n *   两者合并后，后端 @PreAuthorize 与前端 v-has-perm/hasPermission 判断一致，\n *   「tab/按钮可见性 + 接口放行」均通过角色授权驱动，不再写死。\n * \n * C-SEC003：菜单权限加载失败时降级为角色级权限 + DEGRADED 标记，\n * 前端可通过此标记显示「权限降级」提示横幅。\n */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService implements IPermissionService {

    /** 权限降级标记：菜单权限加载失败时注入，前端可据此显示警告 */
    public static final String PERM_DEGRADED = "SYS:PERM_DEGRADED";

    private final SysUserService userService;
    private final IMenuService menuService;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    private final Cache<String, List<String>> permissionCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(10000)
            .build();

    public List<String> loadPermissions(String username) {
        List<String> cached = permissionCache.getIfPresent(username);
        if (cached != null) {
            return cached;
        }
        List<String> permissions = loadFromDb(username);
        permissionCache.put(username, permissions);
        return permissions;
    }

    /** 登录成功后强制刷新（让角色变更立即在新会话体现），返回最新权限 */
    public List<String> refresh(String username) {
        List<String> permissions = loadFromDb(username);
        permissionCache.put(username, permissions);
        return permissions;
    }

    /** 登录专用：复用已查询的 SysUser，避免重复 getByUsername */
    public List<String> refresh(SysUser user) {
        List<String> permissions = loadFromDb(user);
        permissionCache.put(user.getUsername(), permissions);
        return permissions;
    }

    /** 缓存失效（供用户管理角色变更后调用） */
    public void evict(String username) {
        permissionCache.invalidate(username);
    }

    private List<String> loadFromDb(String username) {
        SysUser user = userService.getByUsername(username);
        return loadFromDb(user);
    }

    /** 内部统一：接受已查询的 SysUser，避免重复 getByUsername（登录流程优化） */
    private List<String> loadFromDb(SysUser user) {
        if (user == null) {
            return List.of();
        }
        // S1 加固：非 ACTIVE（禁用/删除）用户视为无任何权限。
        // 返回空列表且不回退 token 内嵌权限，@PreAuthorize 将全部拒绝。
        if (!"ACTIVE".equals(user.getStatus())) {
            return List.of();
        }
        Set<String> perms = new LinkedHashSet<>(userService.listPermissions(user.getId()));
        // S-7：将角色编码作为 ROLE_xxx 权限注入，供 @PreAuthorize("hasRole('ADMIN')") 和 SPCAUT 门禁使用
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId()))
                .stream().map(SysUserRole::getRoleId).toList();
        if (!roleIds.isEmpty()) {
            roleMapper.selectBatchIds(roleIds).stream()
                    .map(r -> "ROLE_" + r.getRoleCode())
                    .forEach(perms::add);
        }
        // 合并菜单授权（rx_role_menu/rx_user_menu：页面 perms + 按钮 perms，参照旧项目）
        try {
            perms.addAll(menuService.userMenuPerms(user.getUsername()));
        } catch (Exception e) {
            // C-SEC003：菜单加载失败时降级为角色级权限，注入 DEGRADED 标记供前端识别
            log.error("菜单权限加载失败（降级模式），用户 {} 将仅拥有角色级权限: {}", user.getUsername(), e.getMessage(), e);
            perms.add(PERM_DEGRADED);
        }
        return List.copyOf(perms);
    }
}