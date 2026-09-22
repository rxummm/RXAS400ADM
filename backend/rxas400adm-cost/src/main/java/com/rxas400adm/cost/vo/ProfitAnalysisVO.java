package com.rxas400adm.cost.vo;

import com.rxas400adm.cost.entity.ProfitAnalysis;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ProfitAnalysisVO {

    private Long id;
    private String analysisNo;
    private String cono;
    private String analysisType;
    private String analysisKey;
    private String analysisName;
    private String period;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal grossProfit;
    private BigDecimal profitMargin;
    private Integer orderCount;
    private Integer itemCount;
    private String status;
    private String remark;
    private String createdBy;
    private LocalDateTime createdTime;

    public static ProfitAnalysisVO from(ProfitAnalysis entity) {
        ProfitAnalysisVO vo = new ProfitAnalysisVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
