package com.rxas400adm.quality.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 批次追溯链实体。
 * 对应表：rx_traceability_chain
 */
@Data
@NoArgsConstructor
@TableName("rx_traceability_chain")
public class TraceabilityChain {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String itemCode;

    private String batchNo;

    /** 追溯类型：UPSTREAM/DOWNSTREAM */
    private String traceType;

    /** 来源类型：RAW_MATERIAL/PRODUCTION/INSPECTION */
    private String sourceType;

    private String sourceNo;

    private String targetType;

    private String targetNo;

    private String relationship;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
