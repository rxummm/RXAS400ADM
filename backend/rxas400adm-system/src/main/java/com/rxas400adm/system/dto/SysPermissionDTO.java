package com.rxas400adm.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 权限码写请求 DTO（create/update 共用）。不含 id（服务端自增）。
 */
@Data
public class SysPermissionDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String permissionCode;

    @NotBlank(message = "{validation.notBlank}")
    private String permissionName;

    private String module;

    private String description;
}
