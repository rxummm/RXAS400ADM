package com.rxas400adm.tpm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备台账实体。
 * 对应表：rx_equipment
 */
@Data
@NoArgsConstructor
@TableName("rx_equipment")
public class Equipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String equipmentNo;

    private String cono;

    private String equipmentName;

    private String equipmentType;

    private String manufacturer;

    private String model;

    private String serialNo;

    private String location;

    private String department;

    private LocalDate purchaseDate;

    private LocalDate installDate;

    private LocalDate warrantyExpiry;

    /** 状态：ACTIVE/INACTIVE/MAINTENANCE/SCRAPPED */
    private String status;

    private String responsiblePerson;

    private String remark;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
