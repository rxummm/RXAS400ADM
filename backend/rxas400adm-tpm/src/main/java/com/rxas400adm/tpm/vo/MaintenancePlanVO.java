package com.rxas400adm.tpm.vo;

import com.rxas400adm.tpm.entity.MaintenancePlan;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MaintenancePlanVO {

    private Long id;
    private String planNo;
    private String cono;
    private Long equipmentId;
    private String planName;
    private String maintenanceType;
    private Integer cycleDays;
    private LocalDate nextDueDate;
    private LocalDate lastMaintenanceDate;
    private String responsiblePerson;
    private String checklist;
    private String status;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static MaintenancePlanVO from(MaintenancePlan entity) {
        MaintenancePlanVO vo = new MaintenancePlanVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
