package com.rxas400adm.as400.freight;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FreightCostRuleDTO(
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "规则名称") String ruleName,
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "承运商") String carrier,
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "计价方式(WEIGHT/VOLUME/PIECE)") String costType,
        @NotNull(message = "{validation.notNull}") @Schema(description = "基础价格") BigDecimal basePrice,
        @NotNull(message = "{validation.notNull}") @Schema(description = "单位价格") BigDecimal unitPrice,
        @Schema(description = "最低价格") BigDecimal minPrice,
        @Schema(description = "最高价格(0=不限)") BigDecimal maxPrice,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "描述") String description) {

    public FreightCostRule toEntity() {
        FreightCostRule rule = new FreightCostRule();
        rule.setRuleName(ruleName);
        rule.setCarrier(carrier);
        rule.setCostType(costType);
        rule.setBasePrice(basePrice);
        rule.setUnitPrice(unitPrice);
        rule.setMinPrice(minPrice != null ? minPrice : BigDecimal.ZERO);
        rule.setMaxPrice(maxPrice != null ? maxPrice : BigDecimal.ZERO);
        rule.setEnabled(enabled != null ? enabled : true);
        rule.setDescription(description);
        return rule;
    }
}
