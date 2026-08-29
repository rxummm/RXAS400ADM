package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 操作模板更新入参（id 走路径变量，防伪造）。
 */
@Data
public class OpTemplateUpdateDTO {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称最长 100 字符")
    @Schema(description = "模板名称", example = "系统检查")
    private String name;

    @Size(max = 500, message = "描述最长 500 字符")
    @Schema(description = "模板描述", example = "检查系统状态")
    private String description;

    @NotBlank(message = "模板步骤不能为空")
    @Size(max = 10000, message = "步骤定义过长")
    @Schema(description = "步骤JSON数组", example = "[{\"command\":\"DSPMSG\"}]")
    private String steps;
}