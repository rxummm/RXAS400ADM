package com.rxas400adm.quality.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SPC控制图记录实体。
 * 对应表：rx_spc_record
 */
@Data
@NoArgsConstructor
@TableName("rx_spc_record")
public class SpcRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String itemCode;

    private String qualityChar;

    private Integer subgroupSize;

    private LocalDate sampleDate;

    private Integer subgroupNo;

    private BigDecimal value1;

    private BigDecimal value2;

    private BigDecimal value3;

    private BigDecimal value4;

    private BigDecimal value5;

    private BigDecimal mean;

    private BigDecimal range;

    private BigDecimal ucl;

    private BigDecimal cl;

    private BigDecimal lcl;

    private Integer isOutOfControl;

    private String remark;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
