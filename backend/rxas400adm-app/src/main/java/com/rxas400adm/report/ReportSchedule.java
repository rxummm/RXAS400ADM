package com.rxas400adm.report;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表定时任务（rx_report_schedule，2.5.18）：按 cron 生成 PDF/Excel 并邮件推送。
 * reportType：metrics / executions / capacity；format：xlsx / pdf；
 * serverId 为空时指标/容量报表兜底 instance=1（与手动导出行为一致）。
 */
@Data
@TableName("rx_report_schedule")
public class ReportSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String reportType;

    private String format;

    private Long serverId;

    private Integer days;

    private String cronExpr;

    /** 收件邮箱，逗号/分号/空格分隔 */
    private String recipients;

    private Boolean enabled;

    /** PENDING / RUNNING / SUCCESS / FAILED */
    private String status;

    private LocalDateTime lastRunTime;

    private String lastResult;

    private String createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
