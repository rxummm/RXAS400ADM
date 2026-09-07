package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作模板/场景模式。
 */
@Data
@TableName("rx_op_template")
public class OpTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String steps;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;
}
