package com.rxas400adm.as400.vo;

/**
 * 库存预警行 VO（可用量 < 安全库存）。
 */
public record BpcsInventoryAlertVO(
        String item,
        String description,
        String warehouse,
        String uom,
        long onHand,
        long allocated,
        long onOrder,
        long available,
        long safetyStock,
        long deficit) {
}
