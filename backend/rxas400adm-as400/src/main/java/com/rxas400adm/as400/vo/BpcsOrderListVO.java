package com.rxas400adm.as400.vo;

/**
 * 订单列表行 VO（用于订单列表搜索）。
 */
public record BpcsOrderListVO(
        String cono,
        String orno,
        String custNo,
        String custName,
        String shipTo,
        String orderDate,
        String reqDate,
        String statusLabel,
        int currentStageIndex,
        int lineCount,
        String rawChsts) {
}
