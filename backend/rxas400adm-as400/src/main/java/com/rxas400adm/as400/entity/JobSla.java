package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业 SLA 规则（rx_job_sla）：为关键作业定义预期耗时，配合最近执行对比展示 SLA 达成情况。
 * 执行数据来自 AS400Client.jobSlaExecutions()（Mock 仿真 / JT400 真实作业历史 QSYS2.JOB_LOG_INFO）。
 */
@Data
@TableName("rx_job_sla")
public class JobSla {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作业名（如 ORDNIGHT） */
    private String jobName;

    /** 作业说明/调度名 */
    private String scheduleName;

    /** 预期耗时（秒） */
    private Integer expectedDurationSec;

    /** 允许偏差百分比（超过即视为 BREACHED） */
    private Integer deviationPercent;

    private Boolean enabled;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
