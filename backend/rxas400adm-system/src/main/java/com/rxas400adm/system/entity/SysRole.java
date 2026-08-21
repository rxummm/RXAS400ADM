package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

/**
 * 角色表（rx_role）：ADMIN / OPERATOR / DEVELOPER / VIEWER
 * 参照旧项目 sys_role：新增 sort/status 与菜单授权列表（menuIds 非库字段）。
 */
@Data
@TableName("rx_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    /** 排序 */
    private Integer sort;

    /** 1=启用 0=停用（停用角色=该角色下用户无菜单） */
    private Integer status;

    /** 角色已授权的菜单 ID 列表（非数据库字段，由 rx_role_menu 查出） */
    @TableField(exist = false)
    private List<Long> menuIds;
}
