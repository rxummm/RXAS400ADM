package com.rxas400adm.system.dto;

import lombok.Data;

/**
 * 系统参数写请求 DTO。configKey 由路径变量传入，不随请求体提交。
 */
@Data
public class SysConfigDTO {

    private String configValue;

    private String description;
}
