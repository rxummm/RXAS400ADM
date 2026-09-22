package com.rxas400adm.procurement.vo;

import com.rxas400adm.procurement.entity.PurchaseOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购订单列表 VO。
 */
@Data
public class PurchaseOrderVO {

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

    public static PurchaseOrderVO from(PurchaseOrder entity) {
        PurchaseOrderVO vo = new PurchaseOrderVO();
        vo.setId(entity.getId());
        vo.setPoNo(entity.getPoNo());
        vo.setCono(entity.getCono());
        vo.setVendorCode(entity.getVendorCode());
        vo.setVendorName(entity.getVendorName());
        vo.setOrderDate(entity.getOrderDate());
        vo.setReqDate(entity.getReqDate());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setCurrency(entity.getCurrency());
        vo.setStatus(entity.getStatus());
        vo.setStatusKey("procurement.po.status" + entity.getStatus());
        vo.setApprovalLevel(entity.getApprovalLevel());
        vo.setApprovalStatus(entity.getApprovalStatus());
        vo.setApprovedBy(entity.getApprovedBy());
        vo.setApprovedTime(entity.getApprovedTime());
        vo.setNotes(entity.getNotes());
        vo.setAs400PoNo(entity.getAs400PoNo());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}