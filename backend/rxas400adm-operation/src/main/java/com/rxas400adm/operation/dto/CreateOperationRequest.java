package com.rxas400adm.operation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOperationRequest(
    @NotBlank(message = "operationType is required")
    String operationType,
    String targetType,
    String targetName,
    @Size(max = 10000, message = "requestData must not exceed 10000 characters")
    String requestData,
    String idempotencyKey,
    String requestedBy
) {
}