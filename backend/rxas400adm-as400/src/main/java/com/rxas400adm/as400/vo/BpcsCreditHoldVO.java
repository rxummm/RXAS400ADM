package com.rxas400adm.as400.vo;

/**
 * ⑪ 信用 Hold 管理 VO。
 */
public record BpcsCreditHoldVO(
        String cono,
        String orno,
        String cust,
        String custName,
        String orderDate,
        String reqDate,
        String hid,
        String hstat,
        String crHold,
        String shipHold,
        String prHold
) {
}
