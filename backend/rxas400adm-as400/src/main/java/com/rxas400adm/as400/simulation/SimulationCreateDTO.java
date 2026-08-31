package com.rxas400adm.as400.simulation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SimulationCreateDTO(
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "模拟名称") String simName,
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "物料号") String itemNo,
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "仓库") String warehouse,
        @NotNull(message = "{validation.notNull}") @Schema(description = "当前库存") Integer currentStock,
        @Schema(description = "需求变化百分比(默认100=不变)") BigDecimal demandChange,
        @Schema(description = "提前期(天)") Integer leadTimeDays,
        @Schema(description = "安全库存") Integer safetyStock,
        @Schema(description = "再订货点") Integer reorderPoint) {}
