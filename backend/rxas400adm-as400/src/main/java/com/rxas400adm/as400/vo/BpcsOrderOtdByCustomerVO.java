package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * 按客户聚合的 OTD 绩效 VO。
 */
public record BpcsOrderOtdByCustomerVO(
        String customerNo,
        String customerName,
        int totalOrders,
        int onTimeOrders,
        /** 准时率 */
        BigDecimal otdRate
) {
}
