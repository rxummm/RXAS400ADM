package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rx_shipment_event")
public class ShipmentEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shipmentId;
    private LocalDateTime eventTime;
    private String location;
    private String eventType;
    private String description;
    private String operator;
    private LocalDateTime createdTime;
}
