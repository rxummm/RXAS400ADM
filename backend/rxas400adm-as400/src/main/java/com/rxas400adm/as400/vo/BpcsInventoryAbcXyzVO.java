package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * ABC/XYZ 矩阵分析 VO。
 * ABC 按库存价值分类，XYZ 按需求变异系数分类。
 */
public record BpcsInventoryAbcXyzVO(
        String item,
        String itemDesc,
        int totalQty,
        BigDecimal stockValue,
        /** ABC 分类：A/B/C */
        String abcClass,
        /** 需求均值 */
        BigDecimal avgDemand,
        /** 变异系数 CV */
        BigDecimal cv,
        /** XYZ 分类：X/Y/Z */
        String xyzClass,
        /** 综合分类：AX/AY/AZ/BX/... */
        String matrixCell
) {
}
