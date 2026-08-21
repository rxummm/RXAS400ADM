package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权限表（rx_permission）
 */
@Data
@TableName("rx_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String permissionCode;

    private String permissionName;

    private String module;

    /** 权限说明（V24 新增） */
    private String description;
}
