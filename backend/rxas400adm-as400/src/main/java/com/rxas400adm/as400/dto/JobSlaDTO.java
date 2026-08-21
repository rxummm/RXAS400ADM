package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 作业 SLA 规则写请求 DTO（create/update 共用）。
 * 不含 id/createdTime/updatedTime 等服务端托管字段。
 */
@Data
public class JobSlaDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String jobName;

    private String scheduleName;

    private Integer expectedDurationSec;

    private Integer deviationPercent;

    private Boolean enabled;
}
