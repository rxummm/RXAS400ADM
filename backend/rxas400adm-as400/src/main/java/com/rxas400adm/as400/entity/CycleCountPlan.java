package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 循环盘点计划表（rx_cycle_count_plan）。
 */
@Data
@TableName("rx_cycle_count_plan")
public class CycleCountPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planNo;
    private String item;
    private String itemDesc;
    private String warehouse;
    private LocalDate plannedDate;
    private String status;
    private String abcClass;
    private String operator;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
