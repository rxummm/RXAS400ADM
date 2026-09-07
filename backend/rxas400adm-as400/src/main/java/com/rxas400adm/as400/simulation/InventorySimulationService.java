package com.rxas400adm.as400.simulation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.as400.simulation.mapper.InventorySimulationMapper;
import com.rxas400adm.common.util.EntityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class InventorySimulationService {

    private final InventorySimulationMapper simulationMapper;

    public Page<InventorySimulation> list(SimulationQueryDTO query) {
        Page<InventorySimulation> page = new Page<>(query.getAdjustedCurrent(), query.getAdjustedSize());
        LambdaQueryWrapper<InventorySimulation> qw = new LambdaQueryWrapper<InventorySimulation>()
                .like(query.itemNo() != null && !query.itemNo().isBlank(),
                        InventorySimulation::getItemNo, query.itemNo())
                .like(query.warehouse() != null && !query.warehouse().isBlank(),
                        InventorySimulation::getWarehouse, query.warehouse())
                .eq(query.status() != null && !query.status().isBlank(),
                        InventorySimulation::getStatus, query.status())
                .orderByDesc(InventorySimulation::getCreatedTime);
        return simulationMapper.selectPage(page, qw);
    }

    public InventorySimulation create(SimulationCreateDTO dto) {
        InventorySimulation sim = new InventorySimulation();
        sim.setSimName(dto.simName());
        sim.setItemNo(dto.itemNo());
        sim.setWarehouse(dto.warehouse());
        sim.setCurrentStock(dto.currentStock());
        sim.setDemandChange(dto.demandChange() != null ? dto.demandChange() : BigDecimal.valueOf(100));
        sim.setLeadTimeDays(dto.leadTimeDays() != null ? dto.leadTimeDays() : 7);
        sim.setSafetyStock(dto.safetyStock() != null ? dto.safetyStock() : 0);
        sim.setReorderPoint(dto.reorderPoint() != null ? dto.reorderPoint() : 0);
        sim.setStatus("DRAFT");
        simulationMapper.insert(sim);
        return sim;
    }

    public InventorySimulation run(Long id) {
        InventorySimulation sim = EntityUtil.require(id, "Inventory Simulation", simulationMapper::selectById);
        sim.setStatus("RUNNING");
        simulationMapper.updateById(sim);

        runSimulation(sim);

        sim.setStatus("COMPLETED");
        simulationMapper.updateById(sim);
        return sim;
    }

    private void runSimulation(InventorySimulation sim) {
        int currentStock = sim.getCurrentStock();
        int safetyStock = sim.getSafetyStock();
        int reorderPoint = sim.getReorderPoint();
        int leadTime = sim.getLeadTimeDays();
        double demandFactor = sim.getDemandChange().doubleValue() / 100.0;

        int stockoutDays = 0;
        int reorderCount = 0;
        int totalStock = 0;
        int simulationDays = 90;

        int stock = currentStock;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int day = 1; day <= simulationDays; day++) {
            int dailyDemand = Math.max(1, (int) Math.round(10 * demandFactor * (0.8 + rnd.nextDouble() * 0.4)));
            stock -= dailyDemand;
            totalStock += stock;

            if (stock < safetyStock && stock + leadTime * dailyDemand < reorderPoint) {
                reorderCount++;
                stock += leadTime * dailyDemand + safetyStock;
            }

            if (stock < 0) {
                stockoutDays++;
                stock = 0;
            }
        }

        sim.setResultStockoutDays(stockoutDays);
        sim.setResultReorderCount(reorderCount);
        sim.setResultAvgStock(totalStock / simulationDays);
        double serviceLevel = ((double) (simulationDays - stockoutDays) / simulationDays) * 100;
        sim.setResultServiceLevel(BigDecimal.valueOf(serviceLevel).setScale(2, RoundingMode.HALF_UP));
    }

    public void delete(Long id) {
        EntityUtil.require(id, "Inventory Simulation", simulationMapper::selectById);
        simulationMapper.deleteById(id);
    }
}
