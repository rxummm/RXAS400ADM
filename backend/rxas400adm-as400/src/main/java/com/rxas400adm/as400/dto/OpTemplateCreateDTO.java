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

    @NotBlank(message = "template name is required")
    @Size(max = 100, message = "max length is 100 characters")
    @Schema(description = "模板名称", example = "系统检查")
    private String name;

    @Size(max = 500, message = "max length is 500 characters")
    @Schema(description = "模板描述", example = "检查系统状态")
    private String description;

    @Schema(description = "步骤JSON数组", example = "[{\"command\":\"DSPMSG\"}]")
    @NotBlank(message = "template steps are required")
    @Size(max = 10000, message = "step definition too long")
    private String steps;
}