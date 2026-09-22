package com.rxas400adm.procurement.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购订单详情 VO（含行项和审批历史）。
 */
@Data
public class PurchaseOrderDetailVO {

    private Long id;
    private String poNo;
    private String cono;
    private String vendorCode;
    private String vendorName;
    private LocalDate orderDate;
    private LocalDate reqDate;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private String statusKey;
    private Integer approvalLevel;
    private String approvalStatus;
    private String approvedBy;
    private LocalDateTime approvedTime;
    private String notes;
    private String as400PoNo;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    /** 订单行项 */
    private List<PurchaseOrderItemVO> items;

    /** 审批历史 */
    private List<PurchaseApprovalVO> approvals;
}