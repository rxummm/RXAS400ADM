package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * 呆滞物料分析 VO。
 */
public record BpcsInventorySlowMovingVO(
        String item,
        String itemDesc,
        String warehouse,
        int qtyOnHand,
        String unit,
        BigDecimal unitCost,
        /** 库存价值 = qtyOnHand * unitCost */
        BigDecimal stockValue,
        /** 最后交易日期（YYYYMMDD 格式） */
        String lastTxnDate,
        /** 呆滞天数 */
        int idleDays,
        /** 呆滞等级：NEVER_USED / OVER_12M / 6M_12M / 3M_6M */
        String idleLevel
) {
}
