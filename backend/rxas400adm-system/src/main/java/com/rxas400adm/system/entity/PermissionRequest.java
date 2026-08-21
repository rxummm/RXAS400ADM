package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限自助申请（rx_permission_request）：用户可申请权限码或菜单/按钮（menuIds），
 * 管理员审批通过后授权（菜单→rx_user_menu 直接授权 / 权限码→REQUESTED 角色）。
 */
@Data
@TableName("rx_permission_request")
public class PermissionRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 申请权限码（旧方式，可为空） */
    private String permissionCode;

    /** 申请的菜单/按钮 ID 列表（JSON 数组，菜单树方式） */
    private String menuIds;

    /** 申请的菜单名称列表（JSON 数组，审批展示用） */
    private String menuNames;

    private String reason;

    /** PENDING/APPROVED/REJECTED */
    private String status;

    private String approver;

    private String approveComment;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}