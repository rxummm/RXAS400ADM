package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rx_shipment")
public class Shipment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String shipmentNo;
    private String loadNo;
    private String carrier;
    private String trackingNo;
    private String status;
    private LocalDate shipDate;
    private LocalDate eta;
    private LocalDate actualDelivery;
    private String orderNo;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
