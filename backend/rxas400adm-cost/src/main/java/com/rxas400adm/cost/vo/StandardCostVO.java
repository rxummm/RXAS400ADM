package com.rxas400adm.cost.vo;

import com.rxas400adm.cost.entity.StandardCost;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class StandardCostVO {

    private Long id;
    private String itemCode;
    private String itemDesc;
    private String costComponent;
    private BigDecimal standardQty;
    private BigDecimal standardPrice;
    private BigDecimal standardCost;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String status;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public static StandardCostVO from(StandardCost entity) {
        if (entity == null) {
            return null;
        }
        StandardCostVO vo = new StandardCostVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
