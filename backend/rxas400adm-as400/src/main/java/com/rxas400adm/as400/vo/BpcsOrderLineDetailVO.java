package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * 订单行增强详情 VO（⑰ 订单详情增强）。
 * 关联 ECL（订单行）+ ESH（发运行）+ EIN（发票）。
 */
public record BpcsOrderLineDetailVO(
        String cono,
        String orno,
        String orln,
        String item,
        String itemDesc,
        Integer qtyOrdered,
        Integer qtyAllocated,
        Integer qtyShipped,
        Integer qtyInvoiced,
        BigDecimal price,
        /** 发运金额 = price * qtyShipped */
        BigDecimal shipAmount,
        String customerNo,
        String reqDate,
        String orderDate,
        /** 发运日期 */
        String shipDate,
        /** 发运状态 */
        String shipStatus,
        /** 载荷号（运单号） */
        String loadNo,
        /** 发票号 */
        String invoiceNo,
        /** 发票日期 */
        String invoiceDate,
        /** 发票金额 */
        BigDecimal invoiceAmount,
        /** 行状态标签 */
        String statusLabel
) {
}
