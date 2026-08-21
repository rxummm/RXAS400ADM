package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-角色关系表（rx_user_role）
 */
@Data
@TableName("rx_user_role")
public class SysUserRole {

    private Long userId;

    private Long roleId;
}
