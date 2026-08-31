package com.rxas400adm.as400.simulation;

import java.math.BigDecimal;

public record InventorySimulationVO(
        Long id,
        String simName,
        String itemNo,
        String warehouse,
        Integer currentStock,
        BigDecimal demandChange,
        Integer leadTimeDays,
        Integer safetyStock,
        Integer reorderPoint,
        Integer resultStockoutDays,
        Integer resultReorderCount,
        Integer resultAvgStock,
        BigDecimal resultServiceLevel,
        String status) {

    public static InventorySimulationVO from(InventorySimulation e) {
        return new InventorySimulationVO(
                e.getId(), e.getSimName(), e.getItemNo(), e.getWarehouse(),
                e.getCurrentStock(), e.getDemandChange(), e.getLeadTimeDays(),
                e.getSafetyStock(), e.getReorderPoint(),
                e.getResultStockoutDays(), e.getResultReorderCount(),
                e.getResultAvgStock(), e.getResultServiceLevel(), e.getStatus());
    }
}
