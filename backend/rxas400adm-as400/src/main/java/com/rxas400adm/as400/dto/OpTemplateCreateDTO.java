package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 操作模板新建入参（分层准绳：禁止 Entity 直收 @RequestBody）。
 */
@Data
public class OpTemplateCreateDTO {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称最长 100 字符")
    @Schema(description = "模板名称", example = "系统检查")
    private String name;

    @Size(max = 500, message = "描述最长 500 字符")
    @Schema(description = "模板描述", example = "检查系统状态")
    private String description;

    @Schema(description = "步骤JSON数组", example = "[{\"command\":\"DSPMSG\"}]")
    @NotBlank(message = "模板步骤不能为空")
    @Size(max = 10000, message = "步骤定义过长")
    private String steps;
}