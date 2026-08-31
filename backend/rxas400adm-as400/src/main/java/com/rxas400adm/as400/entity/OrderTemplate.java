package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 订单模板表。
 */
@Data
@TableName("rx_order_template")
public class OrderTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateName;
    private String cono;
    private String cust;
    private String shipTo;
    private String remark;
    private String lineJson;
    private Integer useCount;
    private String active;
    private String createdBy;
    private String createdTime;
    private String updatedTime;
}
