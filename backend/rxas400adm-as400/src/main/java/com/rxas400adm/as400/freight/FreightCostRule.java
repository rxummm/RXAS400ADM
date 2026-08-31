package com.rxas400adm.as400.freight;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rx_freight_cost_rule")
public class FreightCostRule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleName;
    private String carrier;
    /** WEIGHT / VOLUME / PIECE */
    private String costType;
    private BigDecimal basePrice;
    private BigDecimal unitPrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean enabled;
    private String description;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public BigDecimal calculate(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) return minPrice != null ? minPrice : BigDecimal.ZERO;
        BigDecimal cost = basePrice.add(unitPrice.multiply(quantity));
        if (minPrice != null && cost.compareTo(minPrice) < 0) cost = minPrice;
        if (maxPrice != null && maxPrice.signum() > 0 && cost.compareTo(maxPrice) > 0) cost = maxPrice;
        return cost;
    }
}
