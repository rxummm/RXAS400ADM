package com.rxas400adm.olap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售分析汇总实体。
 * 对应表：rx_olap_sales_summary
 */
@Data
@NoArgsConstructor
@TableName("rx_olap_sales_summary")
public class OlapSalesSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cono;

    private String period;

    private String itemCode;

    private String itemDesc;

    private BigDecimal totalRevenue;

    private BigDecimal totalCost;

    private BigDecimal totalQty;

    private Integer orderCount;

    private Integer customerCount;

    private BigDecimal avgUnitPrice;

    private LocalDate analysisDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
