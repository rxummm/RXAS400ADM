package com.rxas400adm.mrp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BOM行项实体。
 * 对应表：rx_bom_line
 */
@Data
@NoArgsConstructor
@TableName("rx_bom_line")
public class BomLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bomId;

    private Integer lineNo;

    private String componentItem;

    private String componentDesc;

    private BigDecimal quantity;

    private String uom;

    private BigDecimal scrapRate;

    private BigDecimal efficiency;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
