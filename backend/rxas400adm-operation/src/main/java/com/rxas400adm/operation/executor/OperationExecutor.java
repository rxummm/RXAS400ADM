package com.rxas400adm.operation.executor;

import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.StepResult;

import java.util.List;

public interface OperationExecutor {

    List<String> defineSteps();

    StepResult executeStep(Operation op, String stepCode);

    default StepResult verifyStep(Operation op, String stepCode) {
        return null;
    }

    default void compensate(Operation op, String failedStep) {
    }
}