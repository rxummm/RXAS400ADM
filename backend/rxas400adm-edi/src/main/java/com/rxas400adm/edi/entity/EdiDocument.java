package com.rxas400adm.edi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * EDI文档实体。
 * 对应表：rx_edi_document
 */
@Data
@NoArgsConstructor
@TableName("rx_edi_document")
public class EdiDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String documentNo;

    private String cono;

    private Long partnerId;

    private String partnerCode;

    private String partnerName;

    /** 文档类型：850/855/856/810 */
    private String documentType;

    /** 方向：INBOUND/OUTBOUND */
    private String direction;

    /** 状态：PENDING/PROCESSING/SUCCESS/FAILED/REJECTED */
    private String status;

    private String rawContent;

    private String parsedData;

    private String errorMessage;

    private LocalDateTime processedTime;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
