package com.rxas400adm.tpm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保养计划实体。
 * 对应表：rx_equipment_maintenance_plan
 */
@Data
@NoArgsConstructor
@TableName("rx_equipment_maintenance_plan")
public class MaintenancePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planNo;

    private String cono;

    private Long equipmentId;

    private String planName;

    /** 保养类型：DAILY/WEEKLY/MONTHLY/QUARTERLY/ANNUAL */
    private String maintenanceType;

    private Integer cycleDays;

    private LocalDate nextDueDate;

    private LocalDate lastMaintenanceDate;

    private String responsiblePerson;

    private String checklist;

    /** 状态：ACTIVE/SUSPENDED/COMPLETED */
    private String status;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
