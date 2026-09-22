package com.rxas400adm.finance.ar.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应收账款发票详情 VO（含收款记录）。
 */
@Data
public class ArInvoiceDetailVO {

    private Long id;
    private String invoiceNo;
    private String cono;
    private String customerCode;
    private String customerName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String originPoNo;
    private String originSoNo;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balance;
    private String currency;
    private String status;
    private String statusKey;
    private String agingBucket;
    private String notes;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    /** 收款记录 */
    private List<ArPaymentVO> payments;
}
