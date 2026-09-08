package com.rxas400adm.operation.vo;

import com.rxas400adm.operation.domain.Operation;

import java.time.LocalDateTime;
import java.util.List;

public record OperationVO(
    Long id,
    String operationType,
    String status,
    String currentStep,
    List<String> steps,
    String targetType,
    String targetName,
    String requestData,
    String resultData,
    String errorCode,
    String errorMessage,
    Integer retryCount,
    Integer maxRetry,
    String riskLevel,
    String requestedBy,
    LocalDateTime requestedAt,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {
    public static OperationVO from(Operation op, List<String> steps) {
        return new OperationVO(
            op.getId(),
            op.getOperationType(),
            op.getStatus(),
            op.getCurrentStep(),
            steps,
            op.getTargetType(),
            op.getTargetName(),
            op.getRequestData(),
            op.getResultData(),
            op.getErrorCode(),
            op.getErrorMessage(),
            op.getRetryCount(),
            op.getMaxRetry(),
            op.getRiskLevel(),
            op.getRequestedBy(),
            op.getRequestedAt(),
            op.getStartedAt(),
            op.getCompletedAt()
        );
    }
}