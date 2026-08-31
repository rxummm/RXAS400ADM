package com.rxas400adm.as400.vo;

/**
 * ㉜ 预警规则引擎 VO。
 */
public record BpcsAlertRuleVO(
        String item,
        String description,
        String wh,
        int qtyOnHand,
        int safetyStock,
        int maxStock,
        String alertType
) {
}
