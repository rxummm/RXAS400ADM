package com.rxas400adm.quality.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 不合格品报告(NCR)实体。
 * 对应表：rx_ncr
 */
@Data
@NoArgsConstructor
@TableName("rx_ncr")
public class Ncr {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ncrNo;

    private String cono;

    private Long inspectionId;

    private String itemCode;

    private String itemDesc;

    private String batchNo;

    private BigDecimal qtyRejected;

    private String defectType;

    private String defectDescription;

    /** 处置方式：USE_AS_IS/REWORK/SCRAP/RETURN */
    private String disposition;

    private LocalDate dispositionDate;

    private String rootCause;

    private String correctiveAction;

    private String preventiveAction;

    private String assignedTo;

    private LocalDate dueDate;

    /** 状态：OPEN/IN_PROGRESS/CLOSED/CANCELLED */
    private String status;

    private LocalDate closeDate;

    private String closeRemark;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
