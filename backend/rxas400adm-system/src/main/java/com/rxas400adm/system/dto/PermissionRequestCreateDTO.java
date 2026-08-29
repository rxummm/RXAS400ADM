package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 权限申请创建请求体（替代 Map&lt;String, Object&gt;）。
 * 支持两种模式：菜单树模式（menuIds + menuNames）或权限码模式（permissionCode）。
 */
@Data
public class PermissionRequestCreateDTO {

    @Schema(description = "权限码模式：直接申请某个权限码", example = "USER_EDIT")
    private String permissionCode;

    @Schema(description = "菜单树模式：菜单ID列表", example = "[1, 2, 3]")
    private List<Long> menuIds;

    @Schema(description = "菜单树模式：菜单名称列表（用于展示）", example = "[\"用户管理\", \"角色管理\"]")
    private List<String> menuNames;

    @Schema(description = "申请原因", example = "需要用户管理权限")
    private String reason;
}