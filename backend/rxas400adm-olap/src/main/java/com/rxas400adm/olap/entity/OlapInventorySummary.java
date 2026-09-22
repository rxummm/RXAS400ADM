package com.rxas400adm.olap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存分析汇总实体。
 * 对应表：rx_olap_inventory_summary
 */
@Data
@NoArgsConstructor
@TableName("rx_olap_inventory_summary")
public class OlapInventorySummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cono;

    private String warehouse;

    private String itemCode;

    private String itemDesc;

    private BigDecimal totalQty;

    private BigDecimal totalValue;

    private BigDecimal avgAge;

    private BigDecimal turnoverRate;

    private BigDecimal reorderPoint;

    private BigDecimal safetyStock;

    private LocalDate analysisDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
