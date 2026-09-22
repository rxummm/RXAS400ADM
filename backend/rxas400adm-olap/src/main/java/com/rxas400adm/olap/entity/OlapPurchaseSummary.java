package com.rxas400adm.olap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购分析汇总实体。
 * 对应表：rx_olap_purchase_summary
 */
@Data
@NoArgsConstructor
@TableName("rx_olap_purchase_summary")
public class OlapPurchaseSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cono;

    private String vendorCode;

    private String vendorName;

    private BigDecimal totalAmount;

    private BigDecimal totalQty;

    private Integer orderCount;

    private BigDecimal onTimeRate;

    private BigDecimal qualityRate;

    private BigDecimal avgLeadTime;

    private LocalDate analysisDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
