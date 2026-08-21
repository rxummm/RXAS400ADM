package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.WebhookConfigDTO;
import com.rxas400adm.system.entity.WebhookConfig;
import com.rxas400adm.system.entity.WebhookLog;

import java.util.List;

/**
 * Webhook 管理服务接口（rx_webhook / rx_webhook_log）。
 */
public interface IWebhookService {

    List<WebhookConfig> listAll();

    PageResult<WebhookLog> logPage(int current, int size, Integer success, String webhookName);

    WebhookConfig create(WebhookConfigDTO config, String username);

    WebhookConfig update(Long id, WebhookConfigDTO dto);

    void delete(Long id);

    WebhookConfig toggleEnabled(Long id, Integer enabled);

    boolean test(Long id, String title, String content);

    int sendToAllEnabled(String title, String content);

    int cleanLogs(int keepDays);
}
