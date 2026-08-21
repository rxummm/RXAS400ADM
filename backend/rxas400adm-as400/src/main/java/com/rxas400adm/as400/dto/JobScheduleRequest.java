package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 作业调度任务创建/更新请求（2.4.4）。
 */
@Data
public class JobScheduleRequest {

    @NotBlank(message = "{validation.notBlank}")
    private String name;

    private String description;

    @NotNull(message = "{validation.notNull}")
    private Long serverId;

    @NotBlank(message = "{validation.notBlank}")
    private String scheduleType;

    @NotBlank(message = "{validation.notBlank}")
    private String command;

    @NotBlank(message = "{validation.notBlank}")
    private String cronExpr;

    private Boolean enabled;
}
