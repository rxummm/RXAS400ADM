package com.rxas400adm.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 权限申请创建请求体（替代 Map&lt;String, Object&gt;）。
 * 支持两种模式：菜单树模式（menuIds + menuNames）或权限码模式（permissionCode）。
 */
@Data
public class PermissionRequestCreateDTO {

    /** 权限码模式：直接申请某个权限码 */
    private String permissionCode;

    /** 菜单树模式：菜单 ID 列表 */
    private List<Long> menuIds;

    /** 菜单树模式：菜单名称列表（用于展示） */
    private List<String> menuNames;

    /** 申请原因 */
    private String reason;
}
