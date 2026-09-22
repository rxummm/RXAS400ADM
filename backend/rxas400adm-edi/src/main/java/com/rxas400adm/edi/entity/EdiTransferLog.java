package com.rxas400adm.edi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * EDI传输日志实体。
 * 对应表：rx_edi_transfer_log
 */
@Data
@NoArgsConstructor
@TableName("rx_edi_transfer_log")
public class EdiTransferLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String transferNo;

    private Long documentId;

    private Long partnerId;

    /** 方向：SENT/RECEIVED */
    private String direction;

    private String as2MessageId;

    private Integer as2MdnReceived;

    private String as2MdnType;

    private Integer httpStatus;

    private Integer responseTimeMs;

    private String errorMessage;

    private LocalDateTime sentTime;

    private LocalDateTime receivedTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
