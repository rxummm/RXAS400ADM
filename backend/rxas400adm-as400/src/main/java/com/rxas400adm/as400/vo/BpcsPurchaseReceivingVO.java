package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * 采购收�行 VO（订单数量 vs 已收数量）。
 */
public record BpcsPurchaseReceivingVO(
        String pono,
        String vendorName,
        String orderDate,
        String item,
        String itemDesc,
        int qtyOrdered,
        int qtyReceived,
        int qtyOpen,
        BigDecimal unitPrice,
        String reqDate) {
}
