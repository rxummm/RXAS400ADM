package com.rxas400adm.monitor.dto;

import com.rxas400adm.monitor.alert.AlertRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 告警规则请求体（中-16 合并 Create/Update 双类）：两原类逐字相同（Update 亦要求核心字段必填，
 * 前端提交完整表单），合并为单一 DTO 消除双份维护。输入面与实体解耦，消除 mass-assignment；
 * id 仅取自路径，不可由请求体注入。
 */
public record AlertRuleDTO(
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
