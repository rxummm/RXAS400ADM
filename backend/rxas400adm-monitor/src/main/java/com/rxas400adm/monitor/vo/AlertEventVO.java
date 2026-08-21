package com.rxas400adm.monitor.vo;

import com.rxas400adm.monitor.alert.AlertEvent;

import java.time.LocalDateTime;

/**
 * 告警事件视图（P2-10）：与 AlertEvent 字段一致。
 */
public record AlertEventVO(
        Long id,
        Long instanceId,
        Long ruleId,
        String level,
        String message,
        String status,
        LocalDateTime createdTime) {

    public static AlertEventVO from(AlertEvent e) {
        return new AlertEventVO(
                e.getId(), e.getInstanceId(), e.getRuleId(), e.getLevel(),
                e.getMessage(), e.getStatus(), e.getCreatedTime());
    }
}
