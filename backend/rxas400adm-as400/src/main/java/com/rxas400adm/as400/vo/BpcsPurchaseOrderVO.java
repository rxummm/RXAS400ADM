package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 【AS400 业务增强·P2】采购订单 VO（HPH 头 + HPO 行）。
 */
public record BpcsPurchaseOrderVO(
        String cono,
        String pono,
        String vendor,
        String vendorName,
        /** 订购日期 */
        String orderDate,
        /** 要求交期 */
        String reqDate,
        /** 总金额 */
        BigDecimal totalAmount,
        /** 行数 */
        int lineCount,
        /** 状态码（0=Open, 1=Partial, 2=Complete, 3=Closed） */
        int status,
        String statusKey,
        /** 明细行 */
        List<PurchaseLineVO> lines
) {
    public record PurchaseLineVO(
            String lineNo,
            String item,
            String itemDesc,
            Integer qtyOrdered,
            Integer qtyReceived,
            BigDecimal unitPrice,
            String reqDate
    ) {}
}
