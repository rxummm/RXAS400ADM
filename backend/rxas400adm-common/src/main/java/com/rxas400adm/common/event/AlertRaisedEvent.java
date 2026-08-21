package com.rxas400adm.common.event;

/**
 * 告警事件（跨模块发布）：监控告警（AlertEngine）与作业调度失败（JobScheduleService）
 * 统一发布，由 app 模块的监听器读取 sys_config 的 webhook 配置推送外部。
 * channel：ALL=Webhook+邮件+站内 / WEBHOOK=仅Webhook / EMAIL=仅邮件 / INAPP=仅站内通知 / NONE=不通知（默认 ALL）。
 */
public record AlertRaisedEvent(String level, String source, String message, Long instanceId, String channel) {

    public AlertRaisedEvent {
        channel = channel == null || channel.isBlank() ? "ALL" : channel;
    }

    public AlertRaisedEvent(String level, String source, String message, Long instanceId) {
        this(level, source, message, instanceId, "ALL");
    }

    public boolean notifyWebhook() {
        return "ALL".equalsIgnoreCase(channel) || "WEBHOOK".equalsIgnoreCase(channel);
    }

    public boolean notifyEmail() {
        return "ALL".equalsIgnoreCase(channel) || "EMAIL".equalsIgnoreCase(channel);
    }

    public boolean notifyInApp() {
        return "ALL".equalsIgnoreCase(channel) || "INAPP".equalsIgnoreCase(channel);
    }
}