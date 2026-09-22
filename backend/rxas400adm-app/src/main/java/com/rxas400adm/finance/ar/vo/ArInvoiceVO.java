package com.rxas400adm.finance.ar.vo;

import com.rxas400adm.finance.ar.entity.ArInvoice;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应收账款发票列表 VO。
 */
@Data
public class ArInvoiceVO {

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

    public static ArInvoiceVO from(ArInvoice entity) {
        ArInvoiceVO vo = new ArInvoiceVO();
        vo.setId(entity.getId());
        vo.setInvoiceNo(entity.getInvoiceNo());
        vo.setCono(entity.getCono());
        vo.setCustomerCode(entity.getCustomerCode());
        vo.setCustomerName(entity.getCustomerName());
        vo.setInvoiceDate(entity.getInvoiceDate());
        vo.setDueDate(entity.getDueDate());
        vo.setOriginPoNo(entity.getOriginPoNo());
        vo.setOriginSoNo(entity.getOriginSoNo());
        vo.setSubtotal(entity.getSubtotal());
        vo.setTaxRate(entity.getTaxRate());
        vo.setTaxAmount(entity.getTaxAmount());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setPaidAmount(entity.getPaidAmount());
        vo.setBalance(entity.getBalance());
        vo.setCurrency(entity.getCurrency());
        vo.setStatus(entity.getStatus());
        vo.setStatusKey("ar.status" + entity.getStatus());
        vo.setAgingBucket(entity.getAgingBucket());
        vo.setNotes(entity.getNotes());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
