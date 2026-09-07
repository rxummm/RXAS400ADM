package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysMenuMapper;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.system.vo.MenuVO;
import com.rxas400adm.system.vo.RequestableMenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单树构建与用户授权裁剪（从 MenuService 拆分）：
 * 树构建、用户上下文缓存、角色授权加载、可申请菜单树。
 */
@Service
@RequiredArgsConstructor
public class MenuTreeService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserMapper userMapper;

    /** Caffeine 缓存 username → (user, roles)，TTL 5 分钟 */
    private final Cache<String, UserContext> userContextCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(1000)
            .build();

    /** 用户上下文（供同包 MenuService 引用） */
    public record UserContext(SysUser user, List<SysRole> roles, boolean isAdmin) {}

    /** 全量菜单树（管理端，含停用项） */
    public List<SysMenu> tree() {
        return buildTree(menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort)));
    }

    /** 启用菜单树（status=1），转为前端下发结构 */
    public List<MenuVO> enabledMenuTree() {
        return toMenuVOList(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1)
                .in(SysMenu::getMenuType, 1, 2)
                .orderByAsc(SysMenu::getSort)));
    }

    /**
     * 用户菜单树（供 /auth/menu 下发）：
     * - ADMIN：全部启用菜单
     * - 其他：rx_role_menu 授权菜单 ∪ 递归祖先（CTE），排除 admin_only
     */
    public List<MenuVO> userMenuTree(String username) {
        UserContext ctx = loadUserContext(username);
        if (ctx == null) {
            return List.of();
        }
        List<SysMenu> menus = loadAuthorizedMenus(ctx);
        return toMenuVOList(menus);
    }

    /**
     * 可申请菜单树（权限申请页用）：
     * - 排除 admin_only 子树与权限申请菜单本身
     * - 排除用户已拥有的按钮
     */
    public List<RequestableMenuVO> requestableMenuTree(String username) {
        UserContext ctx = loadUserContext(username);
        if (ctx == null || ctx.isAdmin()) {
            return List.of();
        }
        List<SysMenu> allMenus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1).orderByAsc(SysMenu::getSort));
        Set<Long> excluded = new HashSet<>();
        allMenus.stream().filter(m -> m.getAdminOnly() != null && m.getAdminOnly() == 1)
                .forEach(m -> excluded.addAll(collectDescendants(m.getId(), allMenus)));
        allMenus.stream().filter(m -> "permissionRequest".equals(m.getTitle()))
                .forEach(m -> excluded.add(m.getId()));
        Set<Long> owned = menuMapper.selectAuthorizedMenusByUserId(ctx.user().getId()).stream()
                .filter(m -> m.getMenuType() != null && (m.getMenuType() == 3 || m.getMenuType() == 4))
                .map(SysMenu::getId).collect(Collectors.toSet());
        List<SysMenu> requestable = allMenus.stream()
                .filter(m -> !excluded.contains(m.getId()))
                .filter(m -> !owned.contains(m.getId()))
                .toList();
        return buildTree(requestable).stream().map(this::toRequestableMenuVO).toList();
    }

    /**
     * 用户菜单权限码（供 /auth/menu 合并返回 + PermissionService）：
     * - ADMIN：全部启用菜单的 perms
     * - 其他：rx_role_menu/rx_user_menu 已授权菜单的 perms，排除 admin_only
     */
    public List<String> userMenuPerms(String username) {
        UserContext ctx = loadUserContext(username);
        if (ctx == null) {
            return List.of();
        }
        List<SysMenu> menus = loadAllAuthorizedMenus(ctx);
        return menus.stream()
                .map(SysMenu::getPerms)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    /** 清除用户上下文缓存 */
    public void evictUserContext(String username) {
        userContextCache.invalidate(username);
    }

    /** 按用户角色加载菜单（type 1/2） */
    List<SysMenu> loadAuthorizedMenus(UserContext ctx) {
        if (ctx.isAdmin()) {
            return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, 1)
                    .in(SysMenu::getMenuType, 1, 2)
                    .orderByAsc(SysMenu::getSort));
        } else {
            return menuMapper.selectAuthorizedMenusByUserId(ctx.user().getId()).stream()
                    .filter(m -> m.getAdminOnly() == null || m.getAdminOnly() != 1)
                    .filter(m -> m.getMenuType() == 1 || m.getMenuType() == 2)
                    .toList();
        }
    }

    /** 按用户角色加载全部授权菜单（含按钮 type=3） */
    List<SysMenu> loadAllAuthorizedMenus(UserContext ctx) {
        if (ctx.isAdmin()) {
            return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, 1));
        } else {
            return menuMapper.selectAuthorizedMenusByUserId(ctx.user().getId()).stream()
                    .filter(m -> m.getAdminOnly() == null || m.getAdminOnly() != 1)
                    .toList();
        }
    }

    UserContext loadUserContext(String username) {
        return userContextCache.get(username, k -> {
            SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, k));
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            List<SysRole> roles = loadRolesByUser(user.getId());
            boolean isAdmin = roles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleCode()));
            return new UserContext(user, roles, isAdmin);
        });
    }

    private List<SysRole> loadRolesByUser(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds);
    }

    private Set<Long> collectDescendants(Long parentId, List<SysMenu> allMenus) {
        return MenuTreeSupport.collectDescendants(parentId, allMenus);
    }

    /** 构建树（通用） */
    List<SysMenu> buildTree(List<SysMenu> menus) {
        return MenuTreeSupport.buildTree(menus);
    }

    public List<MenuVO> toMenuVOList(List<SysMenu> menus) {
        return buildTree(menus).stream().map(this::toMenuVO).toList();
    }

    private MenuVO toMenuVO(SysMenu menu) {
        return new MenuVO(
                menu.getPath(),
                menu.getTitle(),
                StringUtils.hasText(menu.getIcon()) ? menu.getIcon() : null,
                menu.getCached() == null || menu.getCached() == 1,
                StringUtils.hasText(menu.getCacheName()) ? menu.getCacheName() : null,
                menu.getChildren() != null && !menu.getChildren().isEmpty()
                        ? menu.getChildren().stream().map(this::toMenuVO).toList()
                        : null
        );
    }

    private RequestableMenuVO toRequestableMenuVO(SysMenu menu) {
        return new RequestableMenuVO(
                menu.getId(),
                menu.getMenuName(),
                menu.getMenuType(),
                menu.getTitle(),
                menu.getPerms(),
                menu.getIcon(),
                menu.getChildren() != null && !menu.getChildren().isEmpty()
                        ? menu.getChildren().stream().map(this::toRequestableMenuVO).toList()
                        : null
        );
    }
}
