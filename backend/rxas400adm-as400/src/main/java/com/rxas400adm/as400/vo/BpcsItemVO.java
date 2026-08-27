package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 【AS400 业务增强·P2】物料主档 VO（IIM + IWI + HPO + SSD 多维度聚合）。
 * 前端通过 Tab 切换展示不同维度数据。
 */
public record BpcsItemVO(
        String item,
        String description,
        String uom,
        String category,
        BigDecimal unitCost,
        BigDecimal listPrice,
        /** 重量(kg) */
        Double weight,
        /** 保质期天数 */
        Integer shelfLife,
        // --- Tab: 库存 ---
        List<WhStockVO> warehouses,
        int totalOnHand,
        int totalAllocated,
        int totalAvailable,
        // --- Tab: 最近采购 ---
        List<RecentPurchaseVO> recentPurchases,
        // --- Tab: 最近销售 ---
        List<RecentSalesVO> recentSales
) {
    /** 仓库库存 */
    public record WhStockVO(
            String wh, String location,
            int onHand, int allocated, int onOrder, int available
    ) {}

    /** 最近采购记录 */
    public record RecentPurchaseVO(
            String pono, String vendorName, String orderDate,
            Integer qty, BigDecimal unitPrice
    ) {}

    /** 最近销售记录 */
    public record RecentSalesVO(
            String orno, String custName, String orderDate,
            Integer qty, BigDecimal unitPrice
    ) {}
}
