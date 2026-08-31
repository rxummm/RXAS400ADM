package com.rxas400adm.as400.collaboration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CollabCreateDTO(
        @NotBlank(message = "{validation.notBlank}") @Schema(description = "订单号") String orderNo,
        @Schema(description = "客户代码") String customerCode,
        @Schema(description = "客户名称") String customerName,
        @Schema(description = "优先级") String priority,
        @Schema(description = "负责人") String assignedTo,
        @Schema(description = "截止日期") LocalDate dueDate,
        @Schema(description = "备注") String notes) {}
