package com.rxas400adm.system.service;

import com.rxas400adm.system.entity.SysMenu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜单树共享算法（中-13 合并三重实现）：
 * - {@link #collectDescendants}：MenuTreeService / UserMenuService 原逐字重复的迭代版子孙收集
 * - {@link #buildTree}：两服务原双胞胎树构建
 * - {@link #collectDescendantButtons}：PermissionRequestService 原 O(n²) 递归变体的迭代化等价实现
 *
 * 仅纯函数，无状态；语义与各原私有实现逐一等价（有 UserMenuServiceTest/MenuTreeServiceTest/
 * PermissionRequestServiceTest 护航）。
 */
final class MenuTreeSupport {

    private MenuTreeSupport() {
    }

    /** 迭代收集 parentId 的全部子孙节点 ID（不含自身），parentId 无对应子树时返回空集 */
    static Set<Long> collectDescendants(Long parentId, List<SysMenu> allMenus) {
        Set<Long> result = new HashSet<>();
        Map<Long, List<SysMenu>> byParent = allMenus.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(SysMenu::getParentId));
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(parentId);
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            for (SysMenu child : byParent.getOrDefault(cur, List.of())) {
                result.add(child.getId());
                stack.push(child.getId());
            }
        }
        return result;
    }

    /** 平铺菜单构建嵌套树：父在集合内则挂 children，否则作为根 */
    static List<SysMenu> buildTree(List<SysMenu> menus) {
        Map<Long, SysMenu> byId = menus.stream().collect(Collectors.toMap(SysMenu::getId, m -> m, (a, b) -> b));
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (menu.getParentId() != null && byId.containsKey(menu.getParentId())) {
                SysMenu parent = byId.get(menu.getParentId());
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(menu);
            } else {
                roots.add(menu);
            }
        }
        return roots;
    }

    /**
     * 收集 parentIds 各自全部子孙中的按钮（menuType=3）。迭代实现，
     * 替换 PermissionRequestService 原递归版本（消除深层树的 O(n²)/栈溢出风险）。
     */
    static Set<Long> collectDescendantButtons(List<SysMenu> allMenus, Iterable<Long> parentIds) {
        Set<Long> buttons = new LinkedHashSet<>();
        Map<Long, SysMenu> byId = allMenus.stream()
                .collect(Collectors.toMap(SysMenu::getId, Function.identity(), (a, b) -> b));
        for (Long parentId : parentIds) {
            for (Long id : collectDescendants(parentId, allMenus)) {
                SysMenu m = byId.get(id);
                if (m != null && m.getMenuType() != null && m.getMenuType() == 3) {
                    buttons.add(id);
                }
            }
        }
        return buttons;
    }
}
