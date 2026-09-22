package com.rxas400adm.tpm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保养记录实体。
 * 对应表：rx_equipment_maintenance_record
 */
@Data
@NoArgsConstructor
@TableName("rx_equipment_maintenance_record")
public class MaintenanceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordNo;

    private Long planId;

    private Long equipmentId;

    private LocalDate maintenanceDate;

    private String maintenanceType;

    private String performedBy;

    private BigDecimal durationHours;

    private String partsUsed;

    private BigDecimal cost;

    private String findings;

    private String actionsTaken;

    private LocalDate nextDueDate;

    /** 状态：SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED */
    private String status;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
