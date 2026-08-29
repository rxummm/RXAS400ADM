package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 作业调度任务创建/更新请求（2.4.4）。
 */
@Data
public class JobScheduleRequest {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "调度任务名称", example = "每日备份")
    private String name;

    @Schema(description = "任务描述", example = "每天凌晨执行备份")
    private String description;

    @NotNull(message = "{validation.notNull}")
    @Schema(description = "服务器ID", example = "1")
    private Long serverId;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "调度类型", example = "CRON")
    private String scheduleType;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "执行命令", example = "SBMJOB CMD(CALL PGM(BACKUP))")
    private String command;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "Cron表达式", example = "0 0 2 * * ?")
    private String cronExpr;

    @Schema(description = "是否启用")
    private Boolean enabled;
}