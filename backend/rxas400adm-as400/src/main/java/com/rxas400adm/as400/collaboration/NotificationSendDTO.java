package com.rxas400adm.as400.collaboration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record NotificationSendDTO(
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "协同ID") Long collaborationId,
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "发送人") String sender,
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "接收人") String recipient,
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "消息内容") String message,
        @Schema(description = "通知渠道(SYSTEM/EMAIL/SMS)") String channel) {}
