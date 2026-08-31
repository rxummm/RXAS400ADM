package com.rxas400adm.as400.vo;

/**
 * ㊳ 运单管理 VO。
 */
public record BpcsShipmentVO(
        String loadNo,
        String carrier,
        String destination,
        String shipDate,
        int lineCount,
        double weight,
        String orderNos
) {
}
