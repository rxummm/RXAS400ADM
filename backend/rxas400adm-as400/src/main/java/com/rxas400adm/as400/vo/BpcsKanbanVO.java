package com.rxas400adm.as400.vo;

/**
 * ⑯ 订单看板视图 VO。
 */
public record BpcsKanbanVO(
        String cono,
        String orno,
        String cust,
        String custName,
        String orderDate,
        String reqDate,
        String status,
        int pendingLines,
        int partialLines,
        int shippedLines
) {
}
