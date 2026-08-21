package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.WebhookConfig;

import java.time.LocalDateTime;

/**
 * Webhook 配置视图（P2-10 模式）：与 WebhookConfig 字段一致。
 * 注意：secret 由 WebhookService.sanitize() 先行掩码（P2-6），此处原样透传。
 */
public record WebhookConfigVO(
        Long id,
        String name,
        String url,
        String secret,
        Integer enabled,
        String description,
        String createdBy,
        LocalDateTime createdTime,
        LocalDateTime updatedTime) {

    public static WebhookConfigVO from(WebhookConfig e) {
        return new WebhookConfigVO(e.getId(), e.getName(), e.getUrl(), e.getSecret(), e.getEnabled(),
                e.getDescription(), e.getCreatedBy(), e.getCreatedTime(), e.getUpdatedTime());
    }
}
