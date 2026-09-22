package com.rxas400adm.procurement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批操作 DTO。
 */
@Data
public class PurchaseApprovalActionDTO {

    @NotNull(message = "采购订单 ID 不能为空")
    @Schema(description = "采购订单 ID")
    private Long poId;

    @NotBlank(message = "审批动作不能为空")
    @Schema(description = "动作：APPROVED/REJECTED/RETURNED")
    private String action;

    @Schema(description = "审批意见")
    private String comment;
}