package com.rxas400adm.email.controller;

import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.email.service.IEmailLogService;
import com.rxas400adm.email.vo.EmailLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 邮件发送日志查询。
 */
@RestController
@RequestMapping("/api/v1/email/logs")
@RequiredArgsConstructor
public class EmailLogController {

    private final IEmailLogService emailLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('EMAIL_VIEW')")
    public ApiResponse<PageResult<EmailLogVO>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(emailLogService.page(current, size, channel, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMAIL_VIEW')")
    public ApiResponse<EmailLogVO> detail(@PathVariable Long id) {
        return ApiResponse.success(emailLogService.detail(id));
    }
}
