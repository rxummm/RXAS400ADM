package com.rxas400adm.mrp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MRP建议订单实体。
 * 对应表：rx_mrp_recommendation
 */
@Data
@NoArgsConstructor
@TableName("rx_mrp_recommendation")
public class MrpRecommendation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recommendationNo;

    private String cono;

    private Long demandId;

    private String itemCode;

    private String itemDesc;

    private BigDecimal recommendQty;

    /** 建议类型：PURCHASE/PRODUCTION/TRANSFER */
    private String recommendType;

    private Integer leadTimeDays;

    private LocalDate suggestedDate;

    private String orderNo;

    /** 状态：PENDING/RELEASED/CANCELLED */
    private String status;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
