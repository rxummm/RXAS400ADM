package com.rxas400adm.quality.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 质量检验记录实体。
 * 对应表：rx_quality_inspection
 */
@Data
@NoArgsConstructor
@TableName("rx_quality_inspection")
public class QualityInspection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String inspectionNo;

    private String cono;

    /** 检验类型：IQC/IPQC/OQC */
    private String inspectionType;

    /** 来源类型：PO/SO/PRODUCTION */
    private String sourceType;

    /** 来源单号 */
    private String sourceNo;

    private String itemCode;

    private String itemDesc;

    /** 批次号 */
    private String batchNo;

    private BigDecimal qtyInspected;

    private BigDecimal qtyAccepted;

    private BigDecimal qtyRejected;

    /** 检验结果：PASS/FAIL/PARTIAL */
    private String result;

    private String inspector;

    private LocalDate inspectionDate;

    private String defectCode;

    private String defectDesc;

    /** 关联NCR单号 */
    private String ncrNo;

    private String remark;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
