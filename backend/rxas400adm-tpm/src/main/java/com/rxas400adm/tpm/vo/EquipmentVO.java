package com.rxas400adm.tpm.vo;

import com.rxas400adm.tpm.entity.Equipment;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class EquipmentVO {

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
    private String status;
    private String responsiblePerson;
    private String remark;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static EquipmentVO from(Equipment entity) {
        EquipmentVO vo = new EquipmentVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
