package com.rxas400adm.system.vo;

import java.util.List;

/**
 * 用户菜单数据聚合视图（供 /auth/menu 合并返回：menus/permissions/tabs）。
 * 替代 MenuService 中 {@code Map<String, Object>} 黑盒。
 */
public record UserMenuDataVO(
        List<MenuVO> menus,
        List<String> perms,
        List<TabVO> tabs) {
}