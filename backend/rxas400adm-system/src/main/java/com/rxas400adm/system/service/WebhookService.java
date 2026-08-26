package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.notify.WebhookNotifier;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.security.SecretMasker;
import com.rxas400adm.common.security.SsrfGuard;
import com.rxas400adm.system.dto.WebhookConfigDTO;
import com.rxas400adm.system.entity.WebhookConfig;
import com.rxas400adm.system.entity.WebhookLog;
import com.rxas400adm.system.mapper.WebhookConfigMapper;
import com.rxas400adm.system.mapper.WebhookLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Webhook 管理（rx_webhook / rx_webhook_log）：多端点配置 + 告警/通知分发 + 发送日志。
 */
@Service
@RequiredArgsConstructor
public class WebhookService implements IWebhookService {

    private final WebhookConfigMapper webhookMapper;
    private final WebhookLogMapper webhookLogMapper;
    private final WebhookNotifier webhookNotifier;

    public List<WebhookConfig> listAll() {
        return webhookMapper.selectList(new LambdaQueryWrapper<WebhookConfig>()
                        .orderByDesc(WebhookConfig::getEnabled).orderByAsc(WebhookConfig::getId))
                .stream().map(this::sanitize).toList();
    }

    public PageResult<WebhookLog> logPage(int current, int size, Integer success, String webhookName) {
        LambdaQueryWrapper<WebhookLog> wrapper = new LambdaQueryWrapper<>();
        if (success != null) {
            wrapper.eq(WebhookLog::getSuccess, success);
        }
        if (StringUtils.hasText(webhookName)) {
            wrapper.like(WebhookLog::getWebhookName, webhookName.trim());
        }
        wrapper.orderByDesc(WebhookLog::getCreatedTime);
        Page<WebhookLog> page = webhookLogMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    
    public WebhookConfig create(WebhookConfigDTO dto, String username) {
        WebhookConfig config = new WebhookConfig();
        config.setName(dto.getName());
        config.setUrl(dto.getUrl());
        config.setSecret(dto.getSecret());
        config.setEnabled(dto.getEnabled());
        config.setDescription(dto.getDescription());
        if (!StringUtils.hasText(config.getName()) || !StringUtils.hasText(config.getUrl())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "名称与推送地址必填");
        }
        // L2：SSRF 防护——配置即校验，保存前拒绝内网/回环等不安全推送地址
        SsrfGuard.assertSafeUrl(config.getUrl());
        long exists = webhookMapper.selectCount(new LambdaQueryWrapper<WebhookConfig>()
                .eq(WebhookConfig::getName, config.getName().trim()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Webhook 名称已存在: " + config.getName());
        }
        config.setId(null);
        config.setName(config.getName().trim());
        config.setEnabled(config.getEnabled() == null ? 1 : config.getEnabled());
        config.setCreatedBy(username);
        config.setCreatedTime(LocalDateTime.now());
        config.setUpdatedTime(LocalDateTime.now());
        webhookMapper.insert(config);
        return sanitize(config);
    }

    
    public WebhookConfig update(Long id, WebhookConfigDTO dto) {
        WebhookConfig config = require(id);
        if (StringUtils.hasText(dto.getName())) {
            config.setName(dto.getName().trim());
        }
        if (StringUtils.hasText(dto.getUrl())) {
            config.setUrl(dto.getUrl().trim());
            // L2：SSRF 防护——更新 URL 时同样校验
            SsrfGuard.assertSafeUrl(config.getUrl());
        }
        // P2-6：secret 为空或提交掩码占位（前端未改动）→ 保留旧值；否则覆盖新值
        if (StringUtils.hasText(dto.getSecret()) && !com.rxas400adm.common.security.SecretMasker.isMasked(dto.getSecret())) {
            config.setSecret(dto.getSecret().trim());
        }
        if (dto.getDescription() != null) config.setDescription(dto.getDescription());
        if (dto.getEnabled() != null) config.setEnabled(dto.getEnabled());
        config.setUpdatedTime(LocalDateTime.now());
        webhookMapper.updateById(config);
        return sanitize(config);
    }

    
    public void delete(Long id) {
        webhookMapper.deleteById(require(id).getId());
    }

    /** 切换启用/停用 */
    
    public WebhookConfig toggleEnabled(Long id, Integer enabled) {
        WebhookConfig config = require(id);
        config.setEnabled(enabled);
        config.setUpdatedTime(LocalDateTime.now());
        webhookMapper.updateById(config);
        return sanitize(config);
    }

    /** P2-6：响应脱敏——secret 非空时替换为掩码占位，不回显明文 */
    private WebhookConfig sanitize(WebhookConfig config) {
        if (config != null && config.getSecret() != null && !config.getSecret().isBlank()) {
            config.setSecret(SecretMasker.MASK);
        }
        return config;
    }

    /** 测试推送：对单个配置发送，不落发送日志（便于调试） */
    public boolean test(Long id, String title, String content) {
        WebhookConfig config = require(id);
        return webhookNotifier.push(config.getUrl(), title, content);
    }

    /**
     * 向全部启用 Webhook 推送（告警/通知统一入口），每端点写一条发送日志。
     * 返回成功发送的端点数量。
     */
    public int sendToAllEnabled(String title, String content) {
        List<WebhookConfig> enabled = webhookMapper.selectList(new LambdaQueryWrapper<WebhookConfig>()
                .eq(WebhookConfig::getEnabled, 1));
        int successCount = 0;
        for (WebhookConfig config : enabled) {
            WebhookNotifier.PushResult result = webhookNotifier.pushDetailed(config.getUrl(), title, content);
            WebhookLog logEntry = new WebhookLog();
            logEntry.setWebhookId(config.getId());
            logEntry.setWebhookName(config.getName());
            logEntry.setTitle(title);
            logEntry.setMessage(content);
            logEntry.setSuccess(result.success() ? 1 : 0);
            logEntry.setAttempts(result.attempts());
            logEntry.setErrorMsg(result.errorMsg());
            logEntry.setCreatedTime(LocalDateTime.now());
            webhookLogMapper.insert(logEntry);
            if (result.success()) {
                successCount++;
            }
        }
        return successCount;
    }

    /** 批量清理发送日志（保留最近 keepDays 天） */
    
    public int cleanLogs(int keepDays) {
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(1, keepDays));
        return webhookLogMapper.delete(new LambdaUpdateWrapper<WebhookLog>()
                .lt(WebhookLog::getCreatedTime, before));
    }

    private WebhookConfig require(Long id) {
        WebhookConfig config = webhookMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Webhook 不存在: " + id);
        }
        return config;
    }
}