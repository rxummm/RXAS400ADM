package com.rxas400adm.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 邮件配置批量更新 DTO。
 */
@Data
public class EmailConfigDTO {

    @Schema(description = "邮件配置键值对映射（如 smtp.host → smtp.example.com）")
    private Map<String, String> configs;
}