package com.rxas400adm.finance.ar.vo;

import com.rxas400adm.finance.ar.entity.ArPayment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收款记录 VO。
 */
@Data
public class ArPaymentVO {

    private Long id;
    private String paymentNo;
    private String cono;
    private Long invoiceId;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String paymentMethod;
    private String referenceNo;
    private String receivedBy;
    private String notes;
    private String createdBy;
    private LocalDateTime createdTime;

    public static ArPaymentVO from(ArPayment entity) {
        ArPaymentVO vo = new ArPaymentVO();
        vo.setId(entity.getId());
        vo.setPaymentNo(entity.getPaymentNo());
        vo.setCono(entity.getCono());
        vo.setInvoiceId(entity.getInvoiceId());
        vo.setPaymentDate(entity.getPaymentDate());
        vo.setAmount(entity.getAmount());
        vo.setPaymentMethod(entity.getPaymentMethod());
        vo.setReferenceNo(entity.getReferenceNo());
        vo.setReceivedBy(entity.getReceivedBy());
        vo.setNotes(entity.getNotes());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
