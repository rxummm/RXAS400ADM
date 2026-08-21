package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.system.dto.SysMenuDTO;
import com.rxas400adm.system.entity.SysMenu;
import com.rxas400adm.system.mapper.SysMenuMapper;
import com.rxas400adm.system.vo.MenuVO;
import com.rxas400adm.system.vo.RequestableMenuVO;
import com.rxas400adm.system.vo.TabVO;
import com.rxas400adm.system.vo.UserMenuDataVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单管理 Facade（参照旧项目 SysMenuService）：rx_menu 表驱动动态菜单。
 *
 * <p>已拆分为 2 个内部服务，本类为 Facade：
 * <ul>
 *   <li>{@link MenuTreeService} — 树构建、用户授权裁剪、上下文缓存</li>
 *   <li>{@link MenuManageService} — 菜单 CRUD、环检测、状态切换</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MenuService implements IMenuService {

    private final SysMenuMapper menuMapper;
    private final MenuTreeService treeService;
    private final MenuManageService manageService;

    /* ---------------- 树（委托 MenuTreeService） ---------------- */

    public List<SysMenu> tree() {
        return treeService.tree();
    }

    public List<MenuVO> enabledMenuTree() {
        return treeService.enabledMenuTree();
    }

    public List<MenuVO> userMenuTree(String username) {
        return treeService.userMenuTree(username);
    }

    public List<RequestableMenuVO> requestableMenuTree(String username) {
        return treeService.requestableMenuTree(username);
    }

    public List<String> userMenuPerms(String username) {
        return treeService.userMenuPerms(username);
    }

    /**
     * P4 合并查询：一次 user + roles 查询，返回 {menus, perms, tabs}，
     * 供 /auth/menu 使用，消除 3 次重复 user/role 查询。
     */
    public UserMenuDataVO userMenuData(String username) {
        MenuTreeService.UserContext ctx = treeService.loadUserContext(username);
        if (ctx == null) {
            return new UserMenuDataVO(List.of(), List.of(), List.of());
        }
        List<SysMenu> menus = treeService.loadAuthorizedMenus(ctx);
        List<MenuVO> menuTree = treeService.toMenuVOList(menus);
        List<SysMenu> permMenus = treeService.loadAllAuthorizedMenus(ctx);
        List<String> perms = permMenus.stream()
                .map(SysMenu::getPerms)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        List<SysMenu> tabs = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuType, 4)
                .orderByAsc(SysMenu::getSort));
        if (!ctx.isAdmin()) {
            tabs = tabs.stream()
                    .filter(m -> m.getAdminOnly() == null || m.getAdminOnly() != 1)
                    .toList();
        }
        Map<Long, String> parentTitle = buildParentTitleMap(tabs);
        List<TabVO> tabList = tabs.stream().map(tab -> new TabVO(
                parentTitle.getOrDefault(tab.getParentId(), ""),
                tab.getTitle(),
                tab.getMenuName(),
                tab.getStatus(),
                StringUtils.hasText(tab.getPerms()) ? tab.getPerms() : null
        )).toList();
        return new UserMenuDataVO(menuTree, perms, tabList);
    }

    /** 用户可见 Tab 列表（供 /auth/menu tabs 字段 + 页面级独立查询） */
    public List<TabVO> userTabs(String username) {
        MenuTreeService.UserContext ctx = treeService.loadUserContext(username);
        if (ctx == null) {
            return List.of();
        }
        boolean isAdmin = ctx.isAdmin();
        List<SysMenu> tabs = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuType, 4)
                .orderByAsc(SysMenu::getSort));
        if (!isAdmin) {
            tabs = tabs.stream()
                    .filter(m -> m.getAdminOnly() == null || m.getAdminOnly() != 1)
                    .toList();
        }
        Map<Long, String> parentTitle = buildParentTitleMap(tabs);
        return tabs.stream().map(tab -> new TabVO(
                parentTitle.getOrDefault(tab.getParentId(), ""),
                tab.getTitle(),
                tab.getMenuName(),
                tab.getStatus(),
                StringUtils.hasText(tab.getPerms()) ? tab.getPerms() : null
        )).toList();
    }

    /* ---------------- CRUD（委托 MenuManageService） ---------------- */

    public SysMenu create(SysMenuDTO dto) {
        return manageService.create(dto);
    }

    public SysMenu update(Long id, SysMenuDTO dto) {
        return manageService.update(id, dto);
    }

    public SysMenu toggleStatus(Long id, Integer status) {
        return manageService.toggleStatus(id, status);
    }

    public void delete(Long id) {
        manageService.delete(id);
    }

    /* ---------------- 内部辅助 ---------------- */

    private Map<Long, String> buildParentTitleMap(List<SysMenu> tabs) {
        List<Long> parentIds = tabs.stream()
                .map(SysMenu::getParentId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        return menuMapper.selectBatchIds(parentIds).stream()
                .collect(Collectors.toMap(SysMenu::getId, SysMenu::getTitle));
    }
}
