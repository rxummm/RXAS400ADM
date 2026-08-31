package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rx_carrier")
public class Carrier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String carrierCode;
    private String carrierName;
    private String contact;
    private String phone;
    private String level;
    private String region;
    private String active;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
