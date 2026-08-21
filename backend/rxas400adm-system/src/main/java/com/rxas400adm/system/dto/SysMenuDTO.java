package com.rxas400adm.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 菜单写请求 DTO（create/update 共用）。
 * 不含 id/createdTime/updatedTime/children 等服务端托管字段。
 */
@Data
public class SysMenuDTO {

    private Long parentId;

    @NotBlank(message = "{validation.notBlank}")
    private String menuName;

    @NotNull(message = "{validation.notNull}")
    private Integer menuType;

    private String title;

    private String path;

    private String component;

    private String perms;

    private String icon;

    private Integer sort;

    private Integer visible;

    private Integer status;

    private Integer adminOnly;
}
