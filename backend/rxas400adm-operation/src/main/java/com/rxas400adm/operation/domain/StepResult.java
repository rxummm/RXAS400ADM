package com.rxas400adm.operation.domain;

public record StepResult(
    StepStatus status,
    String ibmiReturnCode,
    String ibmiMessage,
    String errorDetail,
    long durationMs
) {
    public enum StepStatus { SUCCESS, FAILED, SKIPPED }

    public static StepResult success(String ibmiReturnCode, String ibmiMessage, long durationMs) {
        return new StepResult(StepStatus.SUCCESS, ibmiReturnCode, ibmiMessage, null, durationMs);
    }

    public static StepResult failed(String ibmiReturnCode, String ibmiMessage, String errorDetail, long durationMs) {
        return new StepResult(StepStatus.FAILED, ibmiReturnCode, ibmiMessage, errorDetail, durationMs);
    }
}