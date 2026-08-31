package com.rxas400adm.email.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.email.MailMessage;
import com.rxas400adm.email.dto.EmailSendDTO;
import com.rxas400adm.email.service.IEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 独立邮件发送：手动输入收件人/主题/内容发送邮件（支持附件）。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/email/send")
@RequiredArgsConstructor
@Tag(name = "邮件发送")
public class EmailSendController {

    private final IEmailService emailService;

    @GetMapping("/senders")
    @PreAuthorize("hasAuthority('EMAIL_SEND')")
    @Operation(summary = "获取可用发件人列表")
    public ApiResponse<List<String>> listSenders() {
        List<String> senders = new ArrayList<>();
        // 从邮件配置中读取可用发件人
        try {
            var configs = emailService.listConfigs();
            for (var cfg : configs) {
                if ("from".equals(cfg.getConfigKey()) && cfg.getConfigValue() != null && !cfg.getConfigValue().isBlank()) {
                    senders.add(cfg.getConfigValue());
                }
                if ("user".equals(cfg.getConfigKey()) && cfg.getConfigValue() != null && !cfg.getConfigValue().isBlank()) {
                    senders.add(cfg.getConfigValue());
                }
            }
        } catch (Exception e) {
            log.debug("读取邮件配置失败: {}", e.getMessage());
        }
        // 去重
        return ApiResponse.success(senders.stream().distinct().toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMAIL_SEND')")
    @OperateLog(module = "邮件管理", operation = "发送邮件")
    @Operation(summary = "发送邮件（支持附件）")
    public ApiResponse<Void> send(
            @Valid @ModelAttribute EmailSendDTO dto,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment) throws IOException {
        byte[] attachmentData = null;
        String attachmentName = null;
        if (attachment != null && !attachment.isEmpty()) {
            attachmentData = attachment.getBytes();
            attachmentName = attachment.getOriginalFilename();
        }
        MailMessage message = new MailMessage(
                dto.getSubject(), dto.getText(), dto.getRecipients(),
                dto.getPriority(), attachmentName, attachmentData, MailMessage.CHANNEL_MANUAL,
                dto.getSender());
        emailService.send(message);
        return ApiResponse.success(null);
    }
}
