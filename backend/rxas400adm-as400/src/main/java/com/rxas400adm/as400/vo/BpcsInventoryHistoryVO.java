package com.rxas400adm.as400.vo;

/**
 * 库存变动历史行 VO。
 */
public record BpcsInventoryHistoryVO(
        String item,
        String warehouse,
        String type,
        long quantity,
        String referenceNo,
        String date,
        String time,
        String userId) {
}
