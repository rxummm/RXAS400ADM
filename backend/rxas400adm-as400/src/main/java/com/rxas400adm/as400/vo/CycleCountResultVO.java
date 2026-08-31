package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * 循环盘点结果 VO。
 */
public record CycleCountResultVO(
        Long id,
        String planNo,
        String item,
        String warehouse,
        int systemQty,
        int countedQty,
        int difference,
        BigDecimal differenceValue,
        String reason,
        String countedBy,
        String countTime
) {
}
