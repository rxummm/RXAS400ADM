package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.SysPermission;

/**
 * 权限码视图（P2-10 模式）：与 SysPermission 字段一致，隔离 Controller 直返 Entity。
 */
public record SysPermissionVO(
        Long id,
        String permissionCode,
        String permissionName,
        String module,
        String description) {

    public static SysPermissionVO from(SysPermission e) {
        return new SysPermissionVO(e.getId(), e.getPermissionCode(), e.getPermissionName(),
                e.getModule(), e.getDescription());
    }
}
