package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * ㉚ 库存价值核算 VO。
 */
public record BpcsStockValueVO(
        String item,
        String description,
        String wh,
        int qtyOnHand,
        BigDecimal unitCost,
        BigDecimal stockValue
) {
}
