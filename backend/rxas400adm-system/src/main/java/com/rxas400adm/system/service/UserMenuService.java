package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysUserMenu;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysMenuMapper;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysRoleMenuMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserMenuMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户-菜单直接授权（参照旧项目 SysPermissionManageService）：
 * - 用户全部权限 = 角色授权（rx_role_menu）∪ 直接授权（rx_user_menu）
 * - 管理员可对单个用户精确勾选 目录/菜单/按钮/Tab（menuType=1/2/3/4）授权
 * - 可分配树排除 admin_only 子树与已拥有的菜单页（目录/按钮始终保留占位）
 */
@Service
@RequiredArgsConstructor
public class UserMenuService implements IUserMenuService {

    /** 权限申请菜单 title（申请入口菜单本身不可被授权/申请） */
    private static final String PERMISSION_REQUEST_MENU_TITLE = "permissionRequest";

    private final SysUserMenuMapper userMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /** 用户已有菜单 ID（角色授权 ∪ 直接授权）；ADMIN 返回全部启用菜单 */
    public Set<Long> getUserMenuIds(Long userId) {
        Set<Long> ids = new HashSet<>(userMenuMapper.selectMenuIdsByUserId(userId));
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            if (roles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleCode()))) {
                return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                                .eq(SysMenu::getStatus, 1))
                        .stream().map(SysMenu::getId).collect(Collectors.toSet());
            }
            // S6：批量查询角色菜单（避免 inSql 字符串拼接 + N+1 逐角色查库）
            List<Long> menuIds = roleMenuMapper.selectMenuIdsByRoleIds(roleIds);
            if (!menuIds.isEmpty()) {
                menuMapper.selectBatchIds(menuIds)
                        .forEach(m -> ids.add(m.getId()));
            }
        }
        return ids;
    }

    /** 用户直接授权 ID（仅 rx_user_menu） */
    public Set<Long> getUserDirectMenuIds(Long userId) {
        return new HashSet<>(userMenuMapper.selectMenuIdsByUserId(userId));
    }

    /**
     * 可分配权限树（管理员授权弹窗用）：
     * - 排除 admin_only 子树 + 权限申请菜单
     * - 目录(type=1)/按钮(type=3) 始终保留（占位，按钮可单独勾选）
     * - 已拥有的菜单页(type=2) 排除
     */
    public List<SysMenu> getManageableMenuTree(Long userId) {
        List<SysMenu> allMenus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1).orderByAsc(SysMenu::getSort));
        Set<Long> ownedIds = getUserMenuIds(userId);

        Set<Long> excluded = new HashSet<>();
        List<SysMenu> adminOnlyMenus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getAdminOnly, 1));
        for (SysMenu m : adminOnlyMenus) {
            excluded.addAll(collectDescendantIds(m.getId(), allMenus));
        }
        allMenus.stream().filter(m -> PERMISSION_REQUEST_MENU_TITLE.equals(m.getTitle()))
                .forEach(m -> excluded.addAll(collectDescendantIds(m.getId(), allMenus)));

        List<SysMenu> available = allMenus.stream()
                .filter(m -> !excluded.contains(m.getId()))
                .filter(m -> {
                    if (m.getMenuType() != null && m.getMenuType() == 1) return true; // 目录始终保留
                    if (m.getMenuType() != null && (m.getMenuType() == 3 || m.getMenuType() == 4)) return true; // 按钮/Tab 始终保留
                    return !ownedIds.contains(m.getId()); // 排除已拥有的菜单页
                })
                .collect(Collectors.toList());
        return buildTree(available);
    }

    /** 勾选授权（追加模式，幂等） */

    public void addUserMenus(Long userId, List<Long> menuIds) {
        requireUser(userId);
        // P6-① 入参清洗：去 null、去重，空集直接返回
        List<Long> ids = menuIds == null ? List.of()
                : menuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return;
        // P6-② 批量存在性校验（selectBatchIds 一次取回；风格对齐 RoleService.validatedMenuIds）
        Set<Long> existingIds = menuMapper.selectBatchIds(ids).stream()
                .map(SysMenu::getId).collect(Collectors.toSet());
        List<Long> invalid = ids.stream().filter(i -> !existingIds.contains(i)).toList();
        if (!invalid.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "菜单不存在: " + invalid);
        }
        // P6-③ 过滤已授权关联，规避 uk_user_menu 唯一键冲突
        Set<Long> granted = userMenuMapper.selectList(new LambdaQueryWrapper<SysUserMenu>()
                        .eq(SysUserMenu::getUserId, userId).in(SysUserMenu::getMenuId, ids))
                .stream().map(SysUserMenu::getMenuId).collect(Collectors.toSet());
        // P6-④ 剩余项一次 insertBatch，替代逐条 insert 的 N 次 DB 往返
        List<SysUserMenu> toInsert = ids.stream()
                .filter(i -> !granted.contains(i))
                .map(i -> {
                    SysUserMenu um = new SysUserMenu();
                    um.setUserId(userId);
                    um.setMenuId(i);
                    um.setCreatedTime(LocalDateTime.now());
                    return um;
                }).toList();
        if (!toInsert.isEmpty()) {
            userMenuMapper.insertBatch(toInsert);
        }
    }

    /** 移除授权（目录/菜单页移除时连带子孙） */

    public void removeUserMenus(Long userId, List<Long> menuIds) {
        requireUser(userId);
        // P6-⑤ 入参清洗：去 null、去重，空集直接返回
        List<Long> ids = menuIds == null ? List.of()
                : menuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return;
        List<SysMenu> allMenus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1));
        Set<Long> toRemove = new HashSet<>();
        for (Long menuId : ids) {
            toRemove.add(menuId);
            SysMenu menu = allMenus.stream().filter(m -> m.getId().equals(menuId)).findFirst().orElse(null);
            // 目录/菜单页递归移除子孙，按钮只移除自身
            if (menu == null || menu.getMenuType() == 1 || menu.getMenuType() == 2) {
                toRemove.addAll(collectDescendantIds(menuId, allMenus));
            }
        }
        // P6-⑥ 级联展开后一次 IN 批量删除，替代逐条 delete
        if (!toRemove.isEmpty()) {
            userMenuMapper.deleteByUserIdAndMenuIds(userId, toRemove);
        }
    }

    /** 递归收集所有子孙节点 ID（共享实现见 MenuTreeSupport） */
    private Set<Long> collectDescendantIds(Long parentId, List<SysMenu> allMenus) {
        return MenuTreeSupport.collectDescendants(parentId, allMenus);
    }

    private List<SysMenu> buildTree(List<SysMenu> menus) {
        return MenuTreeSupport.buildTree(menus);
    }

    private void requireUser(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在: " + userId);
        }
    }
}