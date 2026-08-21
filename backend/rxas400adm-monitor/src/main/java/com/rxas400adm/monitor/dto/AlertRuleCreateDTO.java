package com.rxas400adm.monitor.dto;

import com.rxas400adm.monitor.alert.AlertRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 告警规则新增请求（P2-10 遗留）：输入面与实体解耦，消除 mass-assignment。
 * 与前端 alertRules 表单字段一致；id 由服务端生成，不可由请求体注入。
 */
public record AlertRuleCreateDTO(
        @NotBlank(message = "{validation.notBlank}") String metricName,
        @NotBlank(message = "{validation.notBlank}") String operator,
        @NotNull(message = "{validation.notNull}") Double threshold,
        Integer durationSeconds,
        @NotBlank(message = "{validation.notBlank}") String level,
        Boolean enabled,
        String channel,
        Long serverId,
        String description) {

    public AlertRule toEntity() {
        AlertRule rule = new AlertRule();
        rule.setMetricName(metricName);
        rule.setOperator(operator);
        rule.setThreshold(threshold);
        rule.setDurationSeconds(durationSeconds);
        rule.setLevel(level);
        rule.setEnabled(enabled);
        rule.setChannel(channel);
        rule.setServerId(serverId);
        rule.setDescription(description);
        return rule;
    }
}
