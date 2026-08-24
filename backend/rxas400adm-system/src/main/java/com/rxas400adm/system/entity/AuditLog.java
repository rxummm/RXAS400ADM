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

    /** 操作对象（如作业名、文件路径、表名等） */
    private String operateTarget;

    /** 操作结果（SUCCESS/FAIL） */
    private String result;

    /** 执行耗时（毫秒） */
    private Long costMs;

    private LocalDateTime createdTime;
}
