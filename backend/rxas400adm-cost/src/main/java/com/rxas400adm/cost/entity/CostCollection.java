package com.rxas400adm.cost.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本归集实体。
 * 对应表：rx_cost_collection
 */
@Data
@NoArgsConstructor
@TableName("rx_cost_collection")
public class CostCollection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String collectionNo;

    private String cono;

    /** 成本类型：MATERIAL/LABOR/OVERHEAD */
    private String costType;

    /** 成本对象类型：ORDER/PRODUCT/DEPARTMENT */
    private String costObjectType;

    private String costObjectNo;

    private String costObjectName;

    /** 成本期间(YYYY-MM) */
    private String period;

    private BigDecimal materialCost;

    private BigDecimal laborCost;

    private BigDecimal overheadCost;

    private BigDecimal totalCost;

    private BigDecimal unitCost;

    private BigDecimal qtyProduced;

    /** 状态：PENDING/POSTED/CLOSED */
    private String status;

    private String remark;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
