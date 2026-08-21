package com.rxas400adm.report;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表定时任务执行历史（rx_report_schedule_history）。
 */
@Data
@TableName("rx_report_schedule_history")
public class ReportScheduleHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scheduleId;

    private LocalDateTime runTime;

    private String status;

    private String message;

    /** 附件大小（字节），失败为 0 */
    private Long fileBytes;

    private LocalDateTime createdTime;
}
