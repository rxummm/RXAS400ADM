package com.rxas400adm.as400.freight;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FreightCostRecordDTO(
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "订单号") String orderNo,
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "承运商") String carrier,
        @Schema(description = "重量(kg)") BigDecimal weight,
        @Schema(description = "体积(m3)") BigDecimal volume,
        @Schema(description = "件数") Integer pieceCount,
        @Schema(description = "预估运费") BigDecimal estimatedCost,
        @Schema(description = "实际运费") BigDecimal actualCost,
        @Schema(description = "发运日期") LocalDate shipDate) {}
