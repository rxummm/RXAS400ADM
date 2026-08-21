package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志表（rx_audit_log）
 */
@Data
@TableName("rx_audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userName;

    private String action;

    private String module;

    private String target;

    private String ip;

    private String detail;

    private LocalDateTime createdTime;
}
