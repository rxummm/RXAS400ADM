package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 订单排程表。
 */
@Data
@TableName("rx_order_schedule")
public class OrderSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String cono;
    private String orno;
    private String cust;
    private String startDate;
    private String endDate;
    private Integer progress;
    private Integer priority;
    private String createdBy;
    private String createdTime;
    private String updatedTime;
}
