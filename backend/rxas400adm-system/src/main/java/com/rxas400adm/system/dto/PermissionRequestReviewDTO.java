package com.rxas400adm.system.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 权限申请审批/驳回入参（强类型替代裸 Map，审批意见可选）。
 */
@Data
public class PermissionRequestReviewDTO {

    @Size(max = 500, message = "审批意见最长 500 字符")
    private String comment;
}
