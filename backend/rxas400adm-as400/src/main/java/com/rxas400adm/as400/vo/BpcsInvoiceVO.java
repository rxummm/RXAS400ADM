package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 【AS400 业务增强·P2】发票轨迹 VO。
 * active tab: BBH/BBL（在制发票）; history tab: SIH/SIL（已开票历史）
 */
public record BpcsInvoiceVO(
        String invNo,
        /** 订单号（关联 ECH） */
        String orno,
        /** 客户号 */
        String cust,
        /** 客户名 */
        String custName,
        /** 发票日期 */
        String invDate,
        /** 状态：active=在制 / posted=已过账 / cancelled=已冲销 */
        String status,
        /** 状态 key（bpcs.invoiceStatus.*） */
        String statusKey,
        /** 总金额 */
        BigDecimal totalAmount,
        /** 税额 */
        BigDecimal taxAmount,
        /** 行数 */
        Integer lineCount,
        /** 明细行 */
        List<InvoiceLineVO> lines
) {
    public record InvoiceLineVO(
            String lineNo,
            String item,
            String itemDesc,
            Integer qty,
            BigDecimal unitPrice,
            BigDecimal lineAmount
    ) {}
}
