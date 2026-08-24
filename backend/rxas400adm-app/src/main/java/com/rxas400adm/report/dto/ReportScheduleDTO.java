package com.rxas400adm.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 报表定时任务写请求 DTO（create/update 共用）。
 * 不含 id/status/createdBy/createdTime/updatedTime 等服务端托管字段，防伪造。
 */
@Data
public class ReportScheduleDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String name;

    @NotBlank(message = "{validation.notBlank}")
    private String reportType;

    /** 导出格式（xlsx/pdf/csv，默认 xlsx） */
    private String format;

    private Long serverId;

    /** 统计天数（默认 7） */
    private Integer days;

    @NotBlank(message = "{validation.notBlank}")
    private String cronExpr;

    /** 收件人邮箱，逗号分隔 */
    private String recipients;

    private Boolean enabled;
}
