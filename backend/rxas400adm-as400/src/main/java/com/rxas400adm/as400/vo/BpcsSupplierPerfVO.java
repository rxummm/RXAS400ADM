package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * 供应商绩效行 VO。
 */
public record BpcsSupplierPerfVO(
        String vendorName,
        int poCount,
        int onTimeCount,
        double onTimeRate,
        BigDecimal avgPrice) {
}
