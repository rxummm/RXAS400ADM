package com.rxas400adm.mrp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MRP需求实体。
 * 对应表：rx_mrp_demand
 */
@Data
@NoArgsConstructor
@TableName("rx_mrp_demand")
public class MrpDemand {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String demandNo;

    private String cono;

    /** 需求类型：SALES_ORDER/FORECAST/Manual */
    private String demandType;

    private String demandSource;

    private String itemCode;

    private String itemDesc;

    private BigDecimal grossRequirement;

    private BigDecimal scheduledReceipt;

    private BigDecimal allocated;

    private BigDecimal onHand;

    private BigDecimal safetyStock;

    private BigDecimal netRequirement;

    private LocalDate requiredDate;

    /** 状态：PENDING/PLANNED/RELEASED/COMPLETED */
    private String status;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
