package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 权限申请审批/驳回入参（强类型替代裸 Map，审批意见可选）。
 */
@Data
public class PermissionRequestReviewDTO {

    @Size(max = 500, message = "审批意见最长 500 字符")
    @Schema(description = "审批意见", example = "同意")
    private String comment;
}