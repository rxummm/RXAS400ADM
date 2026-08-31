package com.rxas400adm.as400.collaboration;

import com.rxas400adm.common.constants.PageConstants;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record CollabQueryDTO(
        @Schema(description = "订单号") String orderNo,
        @Schema(description = "客户代码") String customerCode,
        @Schema(description = "状态") String status,
        @Schema(description = "优先级") String priority,
        @Schema(description = "负责人") String assignedTo,
        @Schema(description = "截止日期起始") LocalDate dueDateFrom,
        @Schema(description = "截止日期结束") LocalDate dueDateTo,
        @Schema(description = "页码") int current,
        @Schema(description = "每页条数") int size) {

    public int getAdjustedCurrent() {
        return (int) PageConstants.clampNum(current);
    }

    public int getAdjustedSize() {
        return (int) PageConstants.clampSize(size);
    }
}
