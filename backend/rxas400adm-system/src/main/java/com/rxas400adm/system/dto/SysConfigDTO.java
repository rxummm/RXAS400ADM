package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 系统参数写请求 DTO。configKey 由路径变量传入，不随请求体提交。
 */
@Data
public class SysConfigDTO {

    @Schema(description = "配置值", example = "true")
    private String configValue;

    @Schema(description = "配置描述", example = "系统开关")
    private String description;
}