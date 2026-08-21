package com.rxas400adm.system.vo;

/**
 * Tab 视图（menu_type=4 的 Tab 项，供 /auth/menu 的 tabs 字段下发）。
 * 替代 MenuService 中 {@code Map<String, Object>} 黑盒。
 */
public record TabVO(
        String page,
        String title,
        String menuName,
        Integer status,
        String perms) {
}