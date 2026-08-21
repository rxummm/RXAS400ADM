package com.rxas400adm.system.vo;

import java.util.List;

/**
 * 可申请菜单树视图（权限申请页用：id/menuName/menuType/perms/icon/children）。
 * 替代 MenuService 中 {@code Map<String, Object>} 黑盒。
 */
public record RequestableMenuVO(
        Long id,
        String menuName,
        Integer menuType,
        String title,
        String perms,
        String icon,
        List<RequestableMenuVO> children) {
}