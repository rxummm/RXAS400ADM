package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * 供应链 KPI 汇总 VO。
 */
public record BpcsKpiVO(
        int totalOrders,
        int closedOrders,
        double completionRate,
        int totalItems,
        BigDecimal inventoryValue,
        double onTimeDeliveryRate) {
}
