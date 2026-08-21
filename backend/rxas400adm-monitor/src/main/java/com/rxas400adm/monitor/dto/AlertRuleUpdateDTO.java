package com.rxas400adm.monitor.dto;

import com.rxas400adm.monitor.alert.AlertRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 告警规则更新请求（P2-10 遗留）：与 Create 分离以便分别校验。
 * 前端更新时提交完整表单（与旧 @RequestBody Entity 行为一致：核心字段必填），
 * id 仅取自路径，杜绝请求体覆盖主键。
 */
public record AlertRuleUpdateDTO(
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
