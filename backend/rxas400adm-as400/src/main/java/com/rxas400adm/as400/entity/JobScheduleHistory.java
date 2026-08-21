package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业调度执行历史（rx_job_schedule_history）。
 */
@Data
@TableName("rx_job_schedule_history")
public class JobScheduleHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scheduleId;

    private LocalDateTime runTime;

    /** SUCCESS / FAILED */
    private String status;

    private String message;

    private Long costMs;
}
