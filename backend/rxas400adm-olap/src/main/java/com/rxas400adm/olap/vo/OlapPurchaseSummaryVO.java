package com.rxas400adm.olap.vo;

import com.rxas400adm.olap.entity.OlapPurchaseSummary;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OlapPurchaseSummaryVO {

    private Long id;
    private String cono;
    private String vendorCode;
    private String vendorName;
    private BigDecimal totalAmount;
    private BigDecimal totalQty;
    private Integer orderCount;
    private BigDecimal onTimeRate;
    private BigDecimal qualityRate;
    private BigDecimal avgLeadTime;
    private LocalDate analysisDate;
    private LocalDateTime createdTime;

    public static OlapPurchaseSummaryVO from(OlapPurchaseSummary entity) {
        OlapPurchaseSummaryVO vo = new OlapPurchaseSummaryVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
