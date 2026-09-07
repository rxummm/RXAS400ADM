package com.rxas400adm.as400.vo;

import java.util.List;

/**
 * 供应链中断预警 VO。
 */
public record BpcsDisruptionAlertVO(
        DisruptionSummary summary,
        List<DisruptionEvent> events,
        List<RiskItem> riskItems
) {
    /** 中断汇总 */
    public record DisruptionSummary(
            int activeAlerts,
            int criticalCount,
            int warningCount,
            int infoCount,
            int resolvedToday
    ) {}

    /** 中断事件 */
    public record DisruptionEvent(
            long id,
            String eventType,
            String severity,
            String title,
            String description,
            String affectedItem,
            String affectedWarehouse,
            String detectedTime,
            String status,
            List<String> impactOrders
    ) {}

    /** 风险物料 */
    public record RiskItem(
            String item,
            String itemDesc,
            String warehouse,
            int daysOfSupply,
            int leadTimeDays,
            String riskLevel,
            String recommendation
    ) {}
}
