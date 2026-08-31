package com.rxas400adm.as400.freight;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FreightCostRecordVO(
        Long id,
        String orderNo,
        String carrier,
        BigDecimal weight,
        BigDecimal volume,
        Integer pieceCount,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        BigDecimal costDiff,
        LocalDate shipDate) {

    public static FreightCostRecordVO from(FreightCostRecord e) {
        return new FreightCostRecordVO(
                e.getId(), e.getOrderNo(), e.getCarrier(), e.getWeight(), e.getVolume(),
                e.getPieceCount(), e.getEstimatedCost(), e.getActualCost(),
                e.getCostDiff(), e.getShipDate());
    }
}
