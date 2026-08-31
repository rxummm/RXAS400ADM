package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AS400用户Profile操作日志表（rx_as400_user_profile_log）
 */
@Data
@TableName("rx_as400_user_profile_log")
public class UserProfileLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String userName;

    /** 操作类型：CREATE/UPDATE/DELETE */
    private String action;

    /** 操作人 */
    private String operator;

    /** 操作详情 */
    private String detail;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
