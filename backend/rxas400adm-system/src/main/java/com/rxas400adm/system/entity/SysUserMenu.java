package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-菜单直接授权（rx_user_menu，参照旧项目 sys_user_menu）：
 * 管理员对单个用户直接授权的菜单/按钮，叠加在角色授权（rx_role_menu）之上。
 */
@Data
@TableName("rx_user_menu")
public class SysUserMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long menuId;

    private LocalDateTime createdTime;
}
