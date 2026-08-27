package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 【AS400 业务增强·P2】库存可用量 VO（IIM 物料主档 + IWI 仓库库存）。
 * 可用量 = 在手 - 已分配 + 在途 + 调整（简化公式）
 */
public record BpcsInventoryVO(
        String item,
        String description,
        String uom,
        /** 全仓汇总 */
        int totalOnHand,
        int totalAllocated,
        int totalOnOrder,
        /** 可用量 = onHand - allocated + onOrder */
        int totalAvailable,
        BigDecimal unitCost,
        /** 各仓库明细 */
        List<WhDetailVO> warehouses
) {
    public record WhDetailVO(
            String wh,
            int onHand,
            int allocated,
            int onOrder,
            int available,
            String location
    ) {}
}
