package com.rxas400adm.olap.vo;

import com.rxas400adm.olap.entity.OlapSalesSummary;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OlapSalesSummaryVO {

    private Long id;
    private String cono;
    private String period;
    private String itemCode;
    private String itemDesc;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal totalQty;
    private Integer orderCount;
    private Integer customerCount;
    private BigDecimal avgUnitPrice;
    private LocalDate analysisDate;
    private LocalDateTime createdTime;

    public static OlapSalesSummaryVO from(OlapSalesSummary entity) {
        OlapSalesSummaryVO vo = new OlapSalesSummaryVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
