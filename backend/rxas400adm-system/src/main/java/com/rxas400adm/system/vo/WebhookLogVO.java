package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.WebhookLog;

import java.time.LocalDateTime;

/**
 * Webhook 发送日志视图（P2-10 模式）：与 WebhookLog 字段一致。
 */
public record WebhookLogVO(
        Long id,
        Long webhookId,
        String webhookName,
        String title,
        String message,
        Integer success,
        Integer attempts,
        String errorMsg,
        LocalDateTime createdTime) {

    public static WebhookLogVO from(WebhookLog e) {
        return new WebhookLogVO(e.getId(), e.getWebhookId(), e.getWebhookName(), e.getTitle(), e.getMessage(),
                e.getSuccess(), e.getAttempts(), e.getErrorMsg(), e.getCreatedTime());
    }
}
