package com.rxas400adm.cost.vo;

import com.rxas400adm.cost.entity.CostVariance;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CostVarianceVO {

    private Long id;
    private String varianceNo;
    private String cono;
    private String itemCode;
    private String itemDesc;
    private String costComponent;
    private BigDecimal standardCost;
    private BigDecimal actualCost;
    private BigDecimal varianceAmount;
    private BigDecimal variancePct;
    private String varianceType;
    private String period;
    private String rootCause;
    private String improvementAction;
    private String status;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static CostVarianceVO from(CostVariance entity) {
        CostVarianceVO vo = new CostVarianceVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
