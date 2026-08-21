package com.rxas400adm.config;

import com.rxas400adm.common.event.AlertRaisedEvent;
import com.rxas400adm.system.service.INotificationService;
import com.rxas400adm.system.service.ISysConfigService;
import com.rxas400adm.system.service.IWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * 告警推送监听器：监控告警与作业调度失败事件（AlertRaisedEvent）统一在这里处理——
 * Webhook 分发（rx_webhook 多端点 + 发送日志）+ 邮件（未配置 SMTP 时内部跳过）
 * + 站内通知（按 channel 定向：ALL/INAPP 才推送，NONE 不推送）。
 *
 * P2-15：Webhook 同步重试（最多 3 次 × 递增间隔）会阻塞告警采集线程——
 * 整个推送流程丢到应用线程池异步执行，采集/告警评估立即返回。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertWebhookListener {

    private final IWebhookService webhookService;
    private final INotificationService notificationService;
    private final EmailNotifier emailNotifier;
    private final ISysConfigService sysConfigService;
    private final Executor alertNotifyPool;

    @EventListener
    public void onAlert(AlertRaisedEvent event) {
        try {
            alertNotifyPool.execute(() -> dispatch(event));
        } catch (java.util.concurrent.RejectedExecutionException e) {
            log.warn("[告警] 通知队列已满，丢弃推送: {}", event.message());
        }
    }

    private void dispatch(AlertRaisedEvent event) {
        boolean isZh = isZh();
        String title = isZh
                ? "RXAS400 告警 [" + event.level() + "] " + event.source()
                : "RXAS400 Alert [" + event.level() + "] " + event.source();
        String content = event.message();
        if (event.notifyWebhook()) {
            try {
                int sent = webhookService.sendToAllEnabled(title, content);
                log.info("[告警] Webhook 分发完成：成功 {} 个端点", sent);
            } catch (Exception e) {
                log.warn("[告警] Webhook 分发失败: {}", e.getMessage());
            }
        }
        if (event.notifyEmail()) {
            try {
                emailNotifier.sendAlert(title, content);
            } catch (Exception e) {
                log.warn("[告警] 邮件通知失败: {}", e.getMessage());
            }
        }
        if (event.notifyInApp()) {
            try {
                notificationService.sendToAllActiveUsers("ALERT", title, content);
            } catch (Exception e) {
                log.warn("[告警] 站内通知失败: {}", e.getMessage());
            }
        } else {
            log.info("[告警] channel={}，跳过站内通知", event.channel());
        }
    }

    private boolean isZh() {
        try {
            String lang = sysConfigService.get("alert.webhook.lang", "zh-CN");
            return "zh-CN".equalsIgnoreCase(lang);
        } catch (Exception e) {
            log.debug("read alert lang config failed, defaulting to zh-CN: {}", e.getMessage());
            return true;
        }
    }
}