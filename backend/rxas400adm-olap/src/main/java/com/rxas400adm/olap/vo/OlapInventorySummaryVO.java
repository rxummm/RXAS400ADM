package com.rxas400adm.olap.vo;

import com.rxas400adm.olap.entity.OlapInventorySummary;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OlapInventorySummaryVO {

    private Long id;
    private String cono;
    private String warehouse;
    private String itemCode;
    private String itemDesc;
    private BigDecimal totalQty;
    private BigDecimal totalValue;
    private BigDecimal avgAge;
    private BigDecimal turnoverRate;
    private BigDecimal reorderPoint;
    private BigDecimal safetyStock;
    private LocalDate analysisDate;
    private LocalDateTime createdTime;

    public static OlapInventorySummaryVO from(OlapInventorySummary entity) {
        OlapInventorySummaryVO vo = new OlapInventorySummaryVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
