package com.rxas400adm.email.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.email.MailMessage;
import com.rxas400adm.email.dto.EmailSendDTO;
import com.rxas400adm.email.service.IEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 独立邮件发送：手动输入收件人/主题/内容发送邮件。
 */
@RestController
@RequestMapping("/api/v1/email/send")
@RequiredArgsConstructor
public class EmailSendController {

    private final IEmailService emailService;

    @PostMapping
    @PreAuthorize("hasAuthority('EMAIL_SEND')")
    @OperateLog(module = "邮件管理", operation = "发送邮件")
    public ApiResponse<Void> send(@Valid @RequestBody EmailSendDTO dto) {
        MailMessage message = MailMessage.manual(
                dto.getSubject(), dto.getText(), dto.getRecipients(), dto.getPriority());
        emailService.send(message);
        return ApiResponse.success(null);
    }
}
