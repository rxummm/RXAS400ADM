package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * 交期绩效 OTD 统计 VO。
 */
public record BpcsOrderOtdStatsVO(
        /** 已交付订单总数 */
        int totalDelivered,
        /** 准时交付数（发运日期 <= 要求日期） */
        int onTime,
        /** 提前交付数 */
        int early,
        /** 延迟交付数 */
        int late,
        /** 准时率 = onTime / totalDelivered * 100 */
        BigDecimal onTimeRate,
        /** 平均延迟天数 */
        BigDecimal avgLateDays
) {
}
