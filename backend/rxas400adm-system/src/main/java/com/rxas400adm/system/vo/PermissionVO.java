package com.rxas400adm.system.vo;

/**
 * 权限码视图（含引用计数：菜单 perms 引用 + 角色绑定）。
 * 替代 PermissionManageService 中 {@code Map<String, Object>} 黑盒。
 */
public record PermissionVO(
        Long id,
        String permissionCode,
        String permissionName,
        String module,
        String description,
        long menuUsage,
        long roleUsage) {
}