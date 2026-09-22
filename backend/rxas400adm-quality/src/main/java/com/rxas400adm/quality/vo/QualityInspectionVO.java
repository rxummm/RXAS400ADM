package com.rxas400adm.quality.vo;

import com.rxas400adm.quality.entity.QualityInspection;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class QualityInspectionVO {

    private Long id;
    private String inspectionNo;
    private String cono;
    private String inspectionType;
    private String sourceType;
    private String sourceNo;
    private String itemCode;
    private String itemDesc;
    private String batchNo;
    private BigDecimal qtyInspected;
    private BigDecimal qtyAccepted;
    private BigDecimal qtyRejected;
    private String result;
    private String inspector;
    private LocalDate inspectionDate;
    private String defectCode;
    private String defectDesc;
    private String ncrNo;
    private String remark;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static QualityInspectionVO from(QualityInspection entity) {
        QualityInspectionVO vo = new QualityInspectionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
