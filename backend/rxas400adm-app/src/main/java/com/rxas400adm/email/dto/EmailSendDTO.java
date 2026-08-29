package com.rxas400adm.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 独立发送邮件 DTO。
 */
@Data
public class EmailSendDTO {

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "邮件主题", example = "系统通知")
    private String subject;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "邮件正文", example = "这是一封测试邮件")
    private String text;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "收件人（逗号分隔）", example = "user@example.com")
    private String recipients;

    @Schema(description = "优先级：HIGH / NORMAL / LOW", example = "NORMAL")
    private String priority;
}