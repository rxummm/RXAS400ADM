package com.rxas400adm.as400.vo;

/**
 * 订单异常检测 VO。
 */
public record BpcsOrderAnomalyVO(
        String cono,
        String orno,
        String cust,
        String custName,
        String orderDate,
        String reqDate,
        String status,
        int backorderLines
) {
}
