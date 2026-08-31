package com.rxas400adm.as400.freight;

import java.math.BigDecimal;

public record FreightCostRuleVO(
        Long id,
        String ruleName,
        String carrier,
        String costType,
        BigDecimal basePrice,
        BigDecimal unitPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean enabled,
        String description) {

    public static FreightCostRuleVO from(FreightCostRule e) {
        return new FreightCostRuleVO(
                e.getId(), e.getRuleName(), e.getCarrier(), e.getCostType(),
                e.getBasePrice(), e.getUnitPrice(), e.getMinPrice(), e.getMaxPrice(),
                e.getEnabled(), e.getDescription());
    }
}
