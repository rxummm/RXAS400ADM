package com.rxas400adm.monitor.dto;

import com.rxas400adm.monitor.alert.AlertRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 告警规则请求体（中-16 合并 Create/Update 双类）：两原类逐字相同（Update 亦要求核心字段必填，
 * 前端提交完整表单），合并为单一 DTO 消除双份维护。输入面与实体解耦，消除 mass-assignment；
 * id 仅取自路径，不可由请求体注入。
 */
public record AlertRuleDTO(
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "指标名称", example = "cpu_usage") String metricName,
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "比较运算符", example = ">") String operator,
        @NotNull(message = "{validation.notNull}") @Schema(description = "阈值", example = "90.0") Double threshold,
        @Schema(description = "持续秒数", example = "300") Integer durationSeconds,
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "告警级别", example = "CRITICAL") String level,
        @Schema(description = "是否启用", example = "true") Boolean enabled,
        @Schema(description = "通知渠道", example = "EMAIL") String channel,
        @Schema(description = "服务器ID", example = "1") Long serverId,
        @Schema(description = "描述", example = "CPU使用率超过90%") String description) {

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