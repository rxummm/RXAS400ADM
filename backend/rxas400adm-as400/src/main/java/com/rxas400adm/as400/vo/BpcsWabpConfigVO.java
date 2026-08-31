package com.rxas400adm.as400.vo;

/**
 * ㊽ WABP 配置 VO。
 */
public record BpcsWabpConfigVO(
        String wh,
        int dayOfWeek,
        String time,
        String shipHold,
        String crHold,
        String prHold,
        String active,
        String maintUser,
        String maintDate
) {
}
