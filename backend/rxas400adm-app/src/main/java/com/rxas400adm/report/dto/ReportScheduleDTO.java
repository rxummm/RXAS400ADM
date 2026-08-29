package com.rxas400adm.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 报表定时任务写请求 DTO（create/update 共用）。
 * 不含 id/status/createdBy/createdTime/updatedTime 等服务端托管字段，防伪造。
 */
@Data
public class ReportScheduleDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "调度任务名称", example = "每日报表")
    private String name;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "报表类型", example = "EXECUTION")
    private String reportType;

    @Schema(description = "导出格式：xlsx/pdf/csv", example = "xlsx")
    private String format;

    @Schema(description = "服务器ID", example = "1")
    private Long serverId;

    @Schema(description = "统计天数", example = "7")
    private Integer days;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "Cron表达式", example = "0 0 8 * * ?")
    private String cronExpr;

    @Schema(description = "收件人邮箱（逗号分隔）", example = "user@example.com")
    private String recipients;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}