package com.rxas400adm.monitor.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 告警规则表（rx_alert_rule，2.5.18 可视化配置）
 * 示例：CPU > 90 持续 300 秒 → Critical
 * channel：ALL=Webhook+邮件+站内 / WEBHOOK=仅Webhook / EMAIL=仅邮件 / INAPP=仅站内 / NONE=不通知；
 * serverId 为空 = 全部服务器，否则仅该服务器（Metric.instanceId = 服务器 ID）。
 */
@Data
@TableName("rx_alert_rule")
public class AlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String metricName;

    private String operator;

    private Double threshold;

    private Integer durationSeconds;

    private String level;

    private Boolean enabled;

    /** ALL / WEBHOOK / EMAIL / NONE（空按 ALL 处理） */
    private String channel;

    /** 空 = 全部服务器，否则仅该服务器 */
    private Long serverId;

    private String description;

    public boolean match(double value) {
        if (Boolean.FALSE.equals(enabled)) {
            return false;
        }
        return switch (operator == null ? ">" : operator) {
            case ">" -> value > threshold;
            case ">=" -> value >= threshold;
            case "<" -> value < threshold;
            case "<=" -> value <= threshold;
            default -> false;
        };
    }
}