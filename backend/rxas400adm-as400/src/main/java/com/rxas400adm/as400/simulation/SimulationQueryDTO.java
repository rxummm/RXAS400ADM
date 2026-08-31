package com.rxas400adm.as400.simulation;

import com.rxas400adm.common.constants.PageConstants;
import io.swagger.v3.oas.annotations.media.Schema;

public record SimulationQueryDTO(
        @Schema(description = "物料号") String itemNo,
        @Schema(description = "仓库") String warehouse,
        @Schema(description = "状态") String status,
        @Schema(description = "页码") int current,
        @Schema(description = "每页条数") int size) {

    public int getAdjustedCurrent() {
        return (int) PageConstants.clampNum(current);
    }

    public int getAdjustedSize() {
        return (int) PageConstants.clampSize(size);
    }
}
