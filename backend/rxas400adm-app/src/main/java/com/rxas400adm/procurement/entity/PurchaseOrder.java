package com.rxas400adm.procurement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购订单主表（rx_purchase_order）。
 * 本地管理表，支持多级审批流和状态跟踪。
 */
@Data
@TableName("rx_purchase_order")
public class PurchaseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 采购单号 */
    private String poNo;

    /** 公司代码 */
    private String cono;

    /** 供应商编码 */
    private String vendorCode;

    /** 供应商名称 */
    private String vendorName;

    /** 下单日期 */
    private LocalDate orderDate;

    /** 要求交货日期 */
    private LocalDate reqDate;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 币种 */
    private String currency;

    /**
     * 状态：DRAFT / PENDING_APPROVAL / APPROVED / SHIPPED / RECEIVED / PAID / CANCELLED
     */
    private String status;

    /** 当前审批层级（0=未提交, 1=采购员, 2=经理, 3=财务） */
    private Integer approvalLevel;

    /** 审批状态：PENDING / APPROVED / REJECTED */
    private String approvalStatus;

    /** 最终审批人 */
    private String approvedBy;

    /** 最终审批时间 */
    private LocalDateTime approvedTime;

    /** 备注 */
    private String notes;

    /** 关联 AS400 PO 号 */
    private String as400PoNo;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}