package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * BOM 展开行 VO。
 */
public record BpcsBomLineVO(
        String parent,
        String component,
        String description,
        BigDecimal qty,
        String uom,
        String effective,
        String expired,
        int onHandQty,
        int allocQty,
        int availQty
) {
}
