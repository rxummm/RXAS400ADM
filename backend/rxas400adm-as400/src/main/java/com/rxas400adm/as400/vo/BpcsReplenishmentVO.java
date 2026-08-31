package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * ② 智能补货建议 VO。
 */
public record BpcsReplenishmentVO(
        String item,
        String description,
        String wh,
        int qtyOnHand,
        int safetyStock,
        int shortage,
        BigDecimal avgDemand
) {
}
