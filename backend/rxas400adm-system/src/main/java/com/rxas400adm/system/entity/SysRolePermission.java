package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-权限关系表（rx_role_permission）
 */
@Data
@TableName("rx_role_permission")
public class SysRolePermission {

    private Long roleId;

    private Long permissionId;
}
