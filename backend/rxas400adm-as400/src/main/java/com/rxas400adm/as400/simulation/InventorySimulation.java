package com.rxas400adm.as400.simulation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rx_inventory_simulation")
public class InventorySimulation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String simName;
    private String itemNo;
    private String warehouse;
    private Integer currentStock;
    private BigDecimal demandChange;
    private Integer leadTimeDays;
    private Integer safetyStock;
    private Integer reorderPoint;
    private Integer resultStockoutDays;
    private Integer resultReorderCount;
    private Integer resultAvgStock;
    private BigDecimal resultServiceLevel;
    /** DRAFT / RUNNING / COMPLETED */
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
