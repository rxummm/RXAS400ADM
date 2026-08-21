package com.rxas400adm.system.dto;

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
    private String name;

    @NotBlank(message = "{validation.notBlank}")
    private String url;

    private String secret;

    private Integer enabled;

    private String description;
}
