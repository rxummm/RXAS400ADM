package com.rxas400adm.cost.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 利润分析实体。
 * 对应表：rx_profit_analysis
 */
@Data
@NoArgsConstructor
@TableName("rx_profit_analysis")
public class ProfitAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String analysisNo;

    private String cono;

    /** 分析类型：PRODUCT/CUSTOMER/REGION */
    private String analysisType;

    private String analysisKey;

    private String analysisName;

    /** 分析期间(YYYY-MM) */
    private String period;

    private BigDecimal totalRevenue;

    private BigDecimal totalCost;

    private BigDecimal grossProfit;

    private BigDecimal profitMargin;

    private Integer orderCount;

    private Integer itemCount;

    /** 状态：PENDING/COMPLETED */
    private String status;

    private String remark;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
