package com.rxas400adm.tpm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备故障记录实体。
 * 对应表：rx_equipment_failure
 */
@Data
@NoArgsConstructor
@TableName("rx_equipment_failure")
public class EquipmentFailure {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String failureNo;

    private String cono;

    private Long equipmentId;

    private LocalDateTime failureDate;

    private String failureType;

    private String description;

    private String rootCause;

    private String repairAction;

    private BigDecimal downtimeHours;

    private BigDecimal repairCost;

    private LocalDateTime resolvedDate;

    /** 状态：OPEN/IN_REPAIR/RESOLVED/CLOSED */
    private String status;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
