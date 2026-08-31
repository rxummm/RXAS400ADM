package com.rxas400adm.as400.freight;

import com.rxas400adm.common.constants.PageConstants;
import io.swagger.v3.oas.annotations.media.Schema;

public record FreightCostQueryDTO(
        @Schema(description = "订单号") String orderNo,
        @Schema(description = "承运商") String carrier,
        @Schema(description = "页码") int current,
        @Schema(description = "每页条数") int size) {

    public int getAdjustedCurrent() {
        return (int) PageConstants.clampNum(current);
    }

    public int getAdjustedSize() {
        return (int) PageConstants.clampSize(size);
    }
}
