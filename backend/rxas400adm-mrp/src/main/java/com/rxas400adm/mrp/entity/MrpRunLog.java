package com.rxas400adm.mrp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MRP运行日志实体。
 * 对应表：rx_mrp_run_log
 */
@Data
@NoArgsConstructor
@TableName("rx_mrp_run_log")
public class MrpRunLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String runNo;

    private String cono;

    private LocalDateTime runDate;

    /** 运行类型：FULL/INCREMENTAL */
    private String runType;

    private Integer scopeItems;

    private Integer netDemandCount;

    private Integer recommendCount;

    private Integer durationSeconds;

    /** 状态：SUCCESS/FAILED */
    private String status;

    private String errorMessage;

    private String createdBy;
}
