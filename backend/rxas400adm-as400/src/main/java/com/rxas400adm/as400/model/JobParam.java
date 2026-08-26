package com.rxas400adm.as400.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 作业操作参数（批量操作用）。
 */
@Data
public class JobParam {

    @NotBlank(message = "作业名不能为空")
    private String jobName;

    @NotBlank(message = "作业用户不能为空")
    private String jobUser;

    @NotBlank(message = "作业编号不能为空")
    private String jobNumber;
}
