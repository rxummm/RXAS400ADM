package com.rxas400adm.as400.vo;

/**
 * Backorder 行明细 VO。
 */
public record BpcsOrderBackorderLineVO(
        String cono,
        String orno,
        String orln,
        String item,
        String itemDesc,
        Integer qtyOrdered,
        Integer qtyShipped,
        Integer qtyAllocated,
        /** 未发运数量 = qtyOrdered - qtyShipped */
        int qtyOpen,
        String customerNo,
        String reqDate,
        String headerStatus
) {
}
