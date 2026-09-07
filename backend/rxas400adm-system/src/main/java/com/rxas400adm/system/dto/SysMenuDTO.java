package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 菜单写请求 DTO（create/update 共用）。
 * 不含 id/createdTime/updatedTime/children 等服务端托管字段。
 */
@Data
public class SysMenuDTO {

    @Schema(description = "父菜单ID", example = "0")
    private Long parentId;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "菜单名称", example = "用户管理")
    private String menuName;

    @NotNull(message = "{validation.notNull}")
    @Schema(description = "菜单类型：0=目录/1=菜单/2=按钮", example = "1")
    private Integer menuType;

    @Schema(description = "菜单标题", example = "用户管理")
    private String title;

    @Schema(description = "路由路径", example = "/system/user")
    private String path;

    @Schema(description = "组件路径", example = "system/user/index")
    private String component;

    @Schema(description = "权限标识", example = "USER_EDIT")
    private String perms;

    @Schema(description = "图标", example = "user")
    private String icon;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "是否可见", example = "1")
    private Integer visible;

    @Schema(description = "状态：1=启用/0=禁用", example = "1")
    private Integer status;

    @Schema(description = "仅管理员可见", example = "0")
    private Integer adminOnly;

    @Schema(description = "是否启用 keep-alive 缓存：1=启用/0=不缓存", example = "1")
    private Integer cached;

    @Schema(description = "keep-alive 组件名（覆盖前端路由 name），为空时取前端路由 name", example = "BpcsOrder")
    private String cacheName;
}