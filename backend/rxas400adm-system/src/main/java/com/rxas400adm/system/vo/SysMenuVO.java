package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.SysMenu;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单视图（P2-10 模式）：与 SysMenu 字段一致（含递归 children），隔离 Controller 直返 Entity。
 */
public record SysMenuVO(
        Long id,
        Long parentId,
        String menuName,
        Integer menuType,
        String title,
        String path,
        String component,
        String perms,
        String icon,
        Integer sort,
        Integer visible,
        Integer status,
        Integer adminOnly,
        LocalDateTime createdTime,
        LocalDateTime updatedTime,
        List<SysMenuVO> children) {

    public static SysMenuVO from(SysMenu m) {
        return new SysMenuVO(m.getId(), m.getParentId(), m.getMenuName(), m.getMenuType(), m.getTitle(),
                m.getPath(), m.getComponent(), m.getPerms(), m.getIcon(), m.getSort(), m.getVisible(),
                m.getStatus(), m.getAdminOnly(), m.getCreatedTime(), m.getUpdatedTime(),
                m.getChildren() == null ? null : m.getChildren().stream().map(SysMenuVO::from).toList());
    }
}
