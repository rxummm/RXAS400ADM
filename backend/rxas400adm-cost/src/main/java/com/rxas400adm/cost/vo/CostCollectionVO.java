package com.rxas400adm.cost.vo;

import com.rxas400adm.cost.entity.CostCollection;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CostCollectionVO {

    private Long id;
    private String collectionNo;
    private String cono;
    private String costType;
    private String costObjectType;
    private String costObjectNo;
    private String costObjectName;
    private String period;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal overheadCost;
    private BigDecimal totalCost;
    private BigDecimal unitCost;
    private BigDecimal qtyProduced;
    private String status;
    private String remark;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static CostCollectionVO from(CostCollection entity) {
        CostCollectionVO vo = new CostCollectionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
