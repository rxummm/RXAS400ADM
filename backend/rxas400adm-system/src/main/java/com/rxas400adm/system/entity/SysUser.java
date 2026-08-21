package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表（rx_user）
 */
@Data
@TableName("rx_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String email;

    /** ACTIVE / DISABLED */
    private String status;

    /** 登录来源：PLATFORM（平台注册）/ AS400（AS400 user profile） */
    private String loginSource;

    /** AS400 登录来源的服务器 ID（login_source=AS400 时有效） */
    private Long as400ServerId;

    private String createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
