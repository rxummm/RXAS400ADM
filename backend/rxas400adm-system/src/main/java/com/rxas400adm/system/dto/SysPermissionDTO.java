package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 权限码写请求 DTO（create/update 共用）。不含 id（服务端自增）。
 */
@Data
public class SysPermissionDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "权限码", example = "USER_EDIT")
    private String permissionCode;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "权限名称", example = "用户编辑")
    private String permissionName;

    @Schema(description = "所属模块", example = "系统管理")
    private String module;

    @Schema(description = "权限描述", example = "允许编辑用户信息")
    private String description;
}