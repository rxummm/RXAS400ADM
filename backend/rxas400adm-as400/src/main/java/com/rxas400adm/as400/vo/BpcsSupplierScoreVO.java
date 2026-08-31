package com.rxas400adm.as400.vo;

/**
 * ④ 供应商评分 VO。
 */
public record BpcsSupplierScoreVO(
        String vendor,
        String vendorName,
        int totalPo,
        int onTime,
        double score
) {
}
