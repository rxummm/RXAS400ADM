package com.rxas400adm.edi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * EDI文档行项实体。
 * 对应表：rx_edi_document_line
 */
@Data
@NoArgsConstructor
@TableName("rx_edi_document_line")
public class EdiDocumentLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Integer lineNo;

    private String itemCode;

    private String itemDesc;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private String uom;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
