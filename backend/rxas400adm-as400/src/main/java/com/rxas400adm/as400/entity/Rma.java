package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 退货 RMA 表。
 */
@Data
@TableName("rx_rma")
public class Rma {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String rmaNo;
    private String cono;
    private String orno;
    private String cust;
    private String item;
    private Integer qty;
    private String reason;
    private String status;
    private String createdBy;
    private String createdTime;
    private String updatedTime;
}
