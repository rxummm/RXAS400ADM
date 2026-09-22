package com.rxas400adm.procurement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购订单行项表（rx_purchase_order_item）。
 */
@Data
@TableName("rx_purchase_order_item")
public class PurchaseOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 采购订单 ID */
    private Long poId;

    /** 行号 */
    private Integer lineNo;

    /** 物料编码 */
    private String itemCode;

    /** 物料描述 */
    private String itemDesc;

    /** 单位 */
    private String uom;

    /** 订购数量 */
    private BigDecimal qtyOrdered;

    /** 已收数量 */
    private BigDecimal qtyReceived;

    /** 已开票数量 */
    private BigDecimal qtyInvoiced;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 行金额 */
    private BigDecimal lineAmount;

    /** 要求到货日期 */
    private LocalDate reqDate;

    /** 实际到货日期 */
    private LocalDate receivedDate;

    /** 行备注 */
    private String notes;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}