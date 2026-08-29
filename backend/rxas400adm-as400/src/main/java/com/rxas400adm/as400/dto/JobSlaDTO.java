package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 作业 SLA 规则写请求 DTO（create/update 共用）。
 * 不含 id/createdTime/updatedTime 等服务端托管字段。
 */
@Data
public class JobSlaDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "作业名称", example = "DAILY_BACKUP")
    private String jobName;

    @Schema(description = "调度名称", example = "DEFAULT_SCHEDULE")
    private String scheduleName;

    @Schema(description = "预期耗时（秒）", example = "300")
    private Integer expectedDurationSec;

    @Schema(description = "允许偏差百分比", example = "20")
    private Integer deviationPercent;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}