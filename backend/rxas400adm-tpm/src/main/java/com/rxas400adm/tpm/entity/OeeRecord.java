package com.rxas400adm.tpm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OEE统计实体。
 * 对应表：rx_equipment_oee
 */
@Data
@NoArgsConstructor
@TableName("rx_equipment_oee")
public class OeeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long equipmentId;

    private LocalDate calcDate;

    private BigDecimal availability;

    private BigDecimal performance;

    private BigDecimal quality;

    private BigDecimal oee;

    private BigDecimal plannedTime;

    private BigDecimal actualRuntime;

    private BigDecimal downtimeMinutes;

    private Integer totalCount;

    private Integer goodCount;

    private Integer defectCount;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
