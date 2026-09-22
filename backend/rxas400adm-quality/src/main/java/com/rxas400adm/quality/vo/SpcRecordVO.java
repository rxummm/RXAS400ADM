package com.rxas400adm.quality.vo;

import com.rxas400adm.quality.entity.SpcRecord;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SpcRecordVO {

    private Long id;
    private String itemCode;
    private String qualityChar;
    private Integer subgroupSize;
    private LocalDate sampleDate;
    private Integer subgroupNo;
    private BigDecimal value1;
    private BigDecimal value2;
    private BigDecimal value3;
    private BigDecimal value4;
    private BigDecimal value5;
    private BigDecimal mean;
    private BigDecimal range;
    private BigDecimal ucl;
    private BigDecimal cl;
    private BigDecimal lcl;
    private Integer isOutOfControl;
    private String remark;
    private String createdBy;
    private LocalDateTime createdTime;

    public static SpcRecordVO from(SpcRecord entity) {
        SpcRecordVO vo = new SpcRecordVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
