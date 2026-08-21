package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.SysRole;

import java.util.List;

/**
 * 角色视图（P2-10 模式）：与 SysRole 字段一致，隔离 Controller 直返 Entity。
 */
public record SysRoleVO(
        Long id,
        String roleCode,
        String roleName,
        String description,
        Integer sort,
        Integer status,
        List<Long> menuIds) {

    public static SysRoleVO from(SysRole e) {
        return new SysRoleVO(e.getId(), e.getRoleCode(), e.getRoleName(), e.getDescription(),
                e.getSort(), e.getStatus(), e.getMenuIds());
    }
}
