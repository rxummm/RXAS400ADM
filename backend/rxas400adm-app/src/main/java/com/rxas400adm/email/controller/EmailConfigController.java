package com.rxas400adm.email.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.email.dto.EmailConfigDTO;
import com.rxas400adm.email.service.IEmailService;
import com.rxas400adm.email.vo.EmailConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import java.util.List;


/**
 * 邮件配置管理（rx_email_config）：SMTP 设置 + 测试发送。
 */
@RestController
@RequestMapping("/api/v1/email/config")
@RequiredArgsConstructor
@Tag(name = "Email Configuration", description = "SMTP settings and test send")
public class EmailConfigController {

    private final IEmailService emailService;

    @GetMapping
    @PreAuthorize("hasAuthority('EMAIL_MANAGE')")
    public ApiResponse<List<EmailConfigVO>> list() {
        return ApiResponse.success(emailService.listConfigs());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('EMAIL_MANAGE')")
    @OperateLog(module = "邮件管理", operation = "更新邮件配置")
    public ApiResponse<Void> update(@Valid @RequestBody EmailConfigDTO dto) {
        emailService.updateConfigs(dto.getConfigs());
        return ApiResponse.success(null);
    }

    @PostMapping("/test-send")
    @PreAuthorize("hasAuthority('EMAIL_MANAGE')")
    @OperateLog(module = "邮件管理", operation = "发送测试邮件")
    public ApiResponse<Void> testSend(@RequestParam String to) {
        emailService.sendTestEmail(to);
        return ApiResponse.success(null);
    }
}