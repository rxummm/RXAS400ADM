package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业调度任务（rx_job_schedule，追踪文档 2.4.4）：定时执行 CL 命令 / SQL。
 * 任务定义持久化在数据库，Quartz 负责按 cron 触发。
 */
@Data
@TableName("rx_job_schedule")
public class JobSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    /** 执行目标 AS400 服务器 ID */
    private Long serverId;

    /** CL / SQL */
    private String scheduleType;

    /** CL 命令或 SQL 语句 */
    private String command;

    /** Quartz cron 表达式 */
    private String cronExpr;

    private Boolean enabled;

    /** PENDING / RUNNING / SUCCESS / FAILED */
    private String status;

    private LocalDateTime lastRunTime;

    private String lastResult;

    private String createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
