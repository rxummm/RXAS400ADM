package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 订单变更记录表。
 */
@Data
@TableName("rx_order_change")
public class OrderChange {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String cono;
    private String orno;
    private String changeType;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String reason;
    private String changedBy;
    private String changedTime;
}
