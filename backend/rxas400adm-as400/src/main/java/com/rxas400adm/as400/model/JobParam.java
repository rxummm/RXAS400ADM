package com.rxas400adm.as400.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 作业操作参数（批量操作用）。
 */
@Data
public class JobParam {

    @NotBlank(message = "job name is required")
    private String jobName;

    @NotBlank(message = "job user is required")
    private String jobUser;

    @NotBlank(message = "job number is required")
    private String jobNumber;
}
