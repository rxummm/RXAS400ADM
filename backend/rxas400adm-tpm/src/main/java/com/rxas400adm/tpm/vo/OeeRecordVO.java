package com.rxas400adm.tpm.vo;

import com.rxas400adm.tpm.entity.OeeRecord;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OeeRecordVO {

    private Long id;
    private Long equipmentId;
    private LocalDate calcDate;
    private BigDecimal availability;
    private BigDecimal performance;
    private BigDecimal quality;
    private BigDecimal oee;
    private BigDecimal plannedTime;
    private BigDecimal actualRuntime;
    private BigDecimal downtimeMinutes;
    private Integer totalCount;
    private Integer goodCount;
    private Integer defectCount;
    private String createdBy;
    private LocalDateTime createdTime;

    public static OeeRecordVO from(OeeRecord entity) {
        OeeRecordVO vo = new OeeRecordVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
