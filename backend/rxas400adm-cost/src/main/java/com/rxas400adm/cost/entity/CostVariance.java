package com.rxas400adm.cost.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本差异分析实体。
 * 对应表：rx_cost_variance
 */
@Data
@NoArgsConstructor
@TableName("rx_cost_variance")
public class CostVariance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String varianceNo;

    private String cono;

    private String itemCode;

    private String itemDesc;

    /** 成本组件：MATERIAL/LABOR/OVERHEAD */
    private String costComponent;

    private BigDecimal standardCost;

    private BigDecimal actualCost;

    private BigDecimal varianceAmount;

    private BigDecimal variancePct;

    /** 差异类型：FAVORABLE/UNFAVORABLE */
    private String varianceType;

    /** 成本期间(YYYY-MM) */
    private String period;

    private String rootCause;

    private String improvementAction;

    /** 状态：OPEN/IN_PROGRESS/CLOSED */
    private String status;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
