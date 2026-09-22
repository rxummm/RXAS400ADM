package com.rxas400adm.approval.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 审批动作 DTO
 */
@Data
public class ApprovalActionDTO {
    /** 通知ID */
    @NotNull
    private Long notificationId;

    /** 审批动作: APPROVED/REJECTED/RETURNED */
    @NotBlank
    private String action;

    /** 审批意见 */
    private String comment;
}
