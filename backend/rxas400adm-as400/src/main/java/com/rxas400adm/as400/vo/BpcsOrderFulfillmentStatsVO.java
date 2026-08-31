package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * 订单履行率统计 VO。
 */
public record BpcsOrderFulfillmentStatsVO(
        /** 总行数 */
        int totalLines,
        /** 已履行行数（发运数量 >= 订购数量） */
        int filledLines,
        /** 行履行率 = filledLines / totalLines * 100 */
        BigDecimal lineFillRate,
        /** 总订购数量 */
        int totalOrdered,
        /** 总发运数量 */
        int totalShipped,
        /** 数量履行率 = totalShipped / totalOrdered * 100 */
        BigDecimal qtyFillRate,
        /** Backorder 行数 */
        int backorderLines,
        /** Backorder 总数量 */
        int backorderQty
) {
}
