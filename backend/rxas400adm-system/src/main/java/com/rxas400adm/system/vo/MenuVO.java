package com.rxas400adm.system.vo;

import java.util.List;

/**
 * 菜单树视图（供前端动态菜单渲染：path/title/icon/children）。
 * 替代 MenuService 中 {@code Map<String, Object>} 黑盒。
 */
public record MenuVO(
        String path,
        String title,
        String icon,
        Boolean cached,
        String cacheName,
        List<MenuVO> children) {
}