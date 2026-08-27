package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * ABC 分析行 VO（按库存价值分类）。
 */
public record BpcsAbcAnalysisVO(
        String item,
        String description,
        String warehouse,
        long quantity,
        BigDecimal unitCost,
        BigDecimal stockValue,
        String abcClass) {
}
