package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Webhook 配置写请求 DTO（create/update 共用）。
 * 不含 id/createdBy/createdTime/updatedTime 等服务端托管字段。
 * secret：更新时为空或掩码占位则保留旧值。
 */
@Data
public class WebhookConfigDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "Webhook名称", example = "通知机器人")
    private String name;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "Webhook URL", example = "https://hooks.example.com/webhook")
    private String url;

    @Schema(description = "密钥（更新时为空则保留旧值）", example = "secret123")
    private String secret;

    @Schema(description = "是否启用", example = "1")
    private Integer enabled;

    @Schema(description = "描述", example = "用于发送通知")
    private String description;
}