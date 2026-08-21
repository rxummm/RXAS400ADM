package com.rxas400adm.system.dto;

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
    private String roleCode;

    @NotBlank(message = "{validation.notBlank}")
    private String roleName;

    private String description;

    private Integer sort;

    private Integer status;

    private List<Long> menuIds;
}
