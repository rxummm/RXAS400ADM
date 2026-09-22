package com.rxas400adm.quality.vo;

import com.rxas400adm.quality.entity.Ncr;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class NcrVO {

    private Long id;
    private String ncrNo;
    private String cono;
    private Long inspectionId;
    private String itemCode;
    private String itemDesc;
    private String batchNo;
    private BigDecimal qtyRejected;
    private String defectType;
    private String defectDescription;
    private String disposition;
    private LocalDate dispositionDate;
    private String rootCause;
    private String correctiveAction;
    private String preventiveAction;
    private String assignedTo;
    private LocalDate dueDate;
    private String status;
    private LocalDate closeDate;
    private String closeRemark;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static NcrVO from(Ncr entity) {
        NcrVO vo = new NcrVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
