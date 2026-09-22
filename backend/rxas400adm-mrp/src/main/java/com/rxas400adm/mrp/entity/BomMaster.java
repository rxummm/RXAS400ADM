package com.rxas400adm.mrp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BOM主表实体。
 * 对应表：rx_bom_master
 */
@Data
@NoArgsConstructor
@TableName("rx_bom_master")
public class BomMaster {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bomNo;

    private String cono;

    private String parentItem;

    private String parentDesc;

    private String bomVersion;

    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    private String status;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
