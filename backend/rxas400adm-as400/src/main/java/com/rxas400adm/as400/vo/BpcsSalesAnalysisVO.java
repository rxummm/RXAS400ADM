package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售分析汇总 VO（客户/物料维度）。
 */
public record BpcsSalesAnalysisVO(
        List<TopEntry> topCustomers,
        List<TopEntry> topItems,
        BigDecimal totalRevenue,
        int totalOrders) {

    /**
     * Top N 条目。
     */
    public record TopEntry(
            String code,
            String name,
            int orderCount,
            BigDecimal totalAmount) {
    }
}
