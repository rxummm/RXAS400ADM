package com.rxas400adm.edi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * EDI伙伴实体。
 * 对应表：rx_edi_partner
 */
@Data
@NoArgsConstructor
@TableName("rx_edi_partner")
public class EdiPartner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String partnerCode;

    private String partnerName;

    /** 类型：SUPPLIER/CUSTOMER */
    private String partnerType;

    private String ediVersion;

    private String as2Url;

    private String as2FromId;

    private String as2ToId;

    private String as2Micalg;

    private String as2Encalgo;

    private String as2CertPath;

    /** 状态：ACTIVE/INACTIVE */
    private String status;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
