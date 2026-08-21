package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-菜单关联表（rx_role_menu）
 */
@Data
@TableName("rx_role_menu")
public class SysRoleMenu {

    private Long roleId;

    private Long menuId;
}
