package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色写请求 DTO（create/update 共用）。不含 id（服务端自增）。
 * menuIds：角色菜单授权，更新时非 null 即整体重建（空数组=清空）。
 */
@Data
public class SysRoleDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "角色编码", example = "ADMIN")
    private String roleCode;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "角色名称", example = "管理员")
    private String roleName;

    @Schema(description = "角色描述", example = "系统管理员角色")
    private String description;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "状态：1=启用/0=禁用", example = "1")
    private Integer status;

    @Schema(description = "菜单ID列表（角色授权）", example = "[1, 2, 3]")
    private List<Long> menuIds;
}