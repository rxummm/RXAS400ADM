package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 多仓库联合补货 VO。
 * 展示同一物料在各仓库的库存分布、安全库存对比、补货建议。
 */
public record BpcsWarehouseReplenishVO(
        String item,
        String itdsc,
        /** 物料总库存（所有仓库合计） */
        BigDecimal totalQty,
        /** 安全库存 */
        BigDecimal safetyStock,
        /** 总库存 vs 安全库存差额（负数=缺口） */
        BigDecimal shortage,
        /** 平均日消耗（基于 ITL 近 90 天） */
        BigDecimal avgDailyDemand,
        /** 建议补货量（缺口 + 安全缓冲） */
        BigDecimal suggestQty,
        /** 各仓库明细 */
        List<WarehouseStockVO> warehouses
) {
    public record WarehouseStockVO(
            String wh,
            /** 在手量 */
            BigDecimal qtyOnHand,
            /** 已分配量 */
            BigDecimal qtyAllocated,
            /** 可用量 */
            BigDecimal qtyAvailable,
            /** 在途量（PO 未收货） */
            BigDecimal qtyOnOrder,
            /** 最后交易日期 */
            String lastTxnDate,
            /** 仓库库存占比 */
            BigDecimal pct
    ) {}
}
