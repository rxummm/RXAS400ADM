package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 【AS400 业务增强·P2】销售趋势 VO（按月聚合）。
 * 供 ECharts 柱线混合图使用。
 */
public record BpcsSalesTrendVO(
        /** 月度数据点列表 */
        List<MonthData> months,
        /** 汇总 */
        BigDecimal totalRevenue,
        int totalOrders,
        int totalLines
) {
    public record MonthData(
            /** YYYY-MM */
            String ym,
            /** 销售额 */
            BigDecimal revenue,
            /** 订单数 */
            int orderCount,
            /** 行数 */
            int lineCount
    ) {}
}
