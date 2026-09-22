package com.rxas400adm.cost.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 标准成本实体。
 * 对应表：rx_standard_cost
 */
@Data
@NoArgsConstructor
@TableName("rx_standard_cost")
public class StandardCost {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String itemCode;

    private String itemDesc;

    /** 成本组件：MATERIAL/LABOR/OVERHEAD */
    private String costComponent;

    private BigDecimal standardQty;

    private BigDecimal standardPrice;

    private BigDecimal standardCost;

    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    /** 状态：ACTIVE/INACTIVE */
    private String status;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
