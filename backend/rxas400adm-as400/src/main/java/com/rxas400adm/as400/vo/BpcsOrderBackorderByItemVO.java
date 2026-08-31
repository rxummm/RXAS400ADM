package com.rxas400adm.as400.vo;

/**
 * Backorder 按物料聚合 VO。
 */
public record BpcsOrderBackorderByItemVO(
        String item,
        String itemDesc,
        /** 该物料的 Backorder 行数 */
        int backorderCount,
        /** 该物料的 Backorder 总数量 */
        int totalBackorderQty
) {
}
