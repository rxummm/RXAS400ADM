package com.rxas400adm.as400.dto;

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
    private String name;

    @Size(max = 500, message = "描述最长 500 字符")
    private String description;

    /** 步骤 JSON 数组（[{command: "..."}]），由服务端校验可解析 */
    @NotBlank(message = "模板步骤不能为空")
    @Size(max = 10000, message = "步骤定义过长")
    private String steps;
}
