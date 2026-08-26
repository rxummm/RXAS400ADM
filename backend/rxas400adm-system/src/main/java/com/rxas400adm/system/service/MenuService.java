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
        // P8 合并查询：授权菜单行集（含 perms 列）只取一次，内存分流构建菜单树与权限码，
        // 消除原先 loadAuthorizedMenus/loadAllAuthorizedMenus 对同一递归 CTE
        // （SysMenuMapper.selectAuthorizedMenusByUserId）的两次调用。admin 直通分支语义不变。
        List<SysMenu> authorizedRows;
        if (ctx.isAdmin()) {
            // admin：全量启用菜单（原两分支分别为 type 1/2 与全类型，取超集后内存过滤）
            authorizedRows = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, 1)
                    .orderByAsc(SysMenu::getSort));
        } else {
            // 非 admin：递归 CTE 仅调 1 次，统一排除 admin_only 子树
            authorizedRows = menuMapper.selectAuthorizedMenusByUserId(ctx.user().getId()).stream()
                    .filter(m -> m.getAdminOnly() == null || m.getAdminOnly() != 1)
                    .toList();
        }
        // 菜单树：仅目录(1)/菜单页(2)，口径同原 loadAuthorizedMenus
        List<MenuVO> menuTree = treeService.toMenuVOList(authorizedRows.stream()
                .filter(m -> m.getMenuType() != null && (m.getMenuType() == 1 || m.getMenuType() == 2))
                .toList());
        // 权限码：全部类型菜单的 perms 去重，口径同原 loadAllAuthorizedMenus
        List<String> perms = authorizedRows.stream()
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
