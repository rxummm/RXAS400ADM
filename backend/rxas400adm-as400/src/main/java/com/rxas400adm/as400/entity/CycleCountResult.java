package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 循环盘点结果表（rx_cycle_count_result）。
 */
@Data
@TableName("rx_cycle_count_result")
public class CycleCountResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;
    private String planNo;
    private String item;
    private String warehouse;
    private int systemQty;
    private int countedQty;
    private int difference;
    private BigDecimal differenceValue;
    private String reason;
    private String countedBy;
    private LocalDateTime countTime;
    private LocalDateTime createdTime;
}
