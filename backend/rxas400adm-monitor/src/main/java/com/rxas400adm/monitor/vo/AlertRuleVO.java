package com.rxas400adm.monitor.vo;

import com.rxas400adm.monitor.alert.AlertRule;

/**
 * 告警规则视图（P2-10）：与 AlertRule 字段一致。
 */
public record AlertRuleVO(
        Long id,
        String metricName,
        String operator,
        Double threshold,
        Integer durationSeconds,
        String level,
        Boolean enabled,
        String channel,
        Long serverId,
        String description) {

    public static AlertRuleVO from(AlertRule e) {
        return new AlertRuleVO(
                e.getId(), e.getMetricName(), e.getOperator(), e.getThreshold(), e.getDurationSeconds(),
                e.getLevel(), e.getEnabled(), e.getChannel(), e.getServerId(), e.getDescription());
    }
}
