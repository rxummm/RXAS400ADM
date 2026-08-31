package com.rxas400adm.as400.freight;

import java.math.BigDecimal;

public record FreightCostTrendVO(
        String month,
        BigDecimal totalCost,
        BigDecimal avgCost,
        int recordCount,
        String carrier) {}
