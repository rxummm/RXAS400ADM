package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.WebhookConfigDTO;
import com.rxas400adm.system.entity.WebhookLog;
import com.rxas400adm.system.service.IWebhookService;
import com.rxas400adm.system.vo.BatchDeleteResultVO;
import com.rxas400adm.system.vo.WebhookConfigVO;
import com.rxas400adm.system.vo.WebhookLogVO;
import com.rxas400adm.system.vo.WebhookTestResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Webhook 管理（rx_webhook / rx_webhook_log）：配置 + 测试 + 发送日志。
 * 权限码：WEBHOOK_MANAGE。
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhook")
public class WebhookController {

    private final IWebhookService webhookService;

    @GetMapping
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    public ApiResponse<List<WebhookConfigVO>> list() {
        return ApiResponse.success(webhookService.listAll().stream().map(WebhookConfigVO::from).toList());
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    public ApiResponse<PageResult<WebhookLogVO>> logs(@RequestParam(defaultValue = "1") int current,
                                                     @RequestParam(defaultValue = "20") int size,
                                                     @RequestParam(required = false) Integer success,
                                                     @RequestParam(required = false) String webhookName) {
        PageResult<WebhookLog> page = webhookService.logPage(current, size, success, webhookName);
        return ApiResponse.success(new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(WebhookLogVO::from).toList()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    @OperateLog(module = "Webhook", operation = "新增 Webhook")
    public ApiResponse<WebhookConfigVO> create(@Valid @RequestBody WebhookConfigDTO config) {
        return ApiResponse.success(WebhookConfigVO.from(webhookService.create(config, currentUsername())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    @OperateLog(module = "Webhook", operation = "修改 Webhook")
    public ApiResponse<WebhookConfigVO> update(@PathVariable Long id, @Valid @RequestBody WebhookConfigDTO dto) {
        return ApiResponse.success(WebhookConfigVO.from(webhookService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    @OperateLog(module = "Webhook", operation = "删除 Webhook")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        webhookService.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/enabled")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    @OperateLog(module = "Webhook", operation = "启停 Webhook")
    public ApiResponse<WebhookConfigVO> toggle(@PathVariable Long id, @RequestParam Integer enabled) {
        return ApiResponse.success(WebhookConfigVO.from(webhookService.toggleEnabled(id, enabled)));
    }

    /** 测试推送（不落发送日志） */
    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    @OperateLog(module = "Webhook", operation = "测试 Webhook")
    public ApiResponse<WebhookTestResultVO> test(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "RXAS400 测试通知") String title,
                                                  @RequestParam(defaultValue = "这是一条来自 RXAS400 平台的测试消息") String content) {
        boolean ok = webhookService.test(id, title, content);
        return ApiResponse.success(new WebhookTestResultVO(ok));
    }

    /** 手动清理发送日志（保留最近 keepDays 天） */
    @DeleteMapping("/logs")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    @OperateLog(module = "Webhook", operation = "清理发送日志")
    public ApiResponse<BatchDeleteResultVO> cleanLogs(@RequestParam(defaultValue = "30") int keepDays) {
        int deleted = webhookService.cleanLogs(keepDays);
        return ApiResponse.success(new BatchDeleteResultVO(deleted));
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }
}