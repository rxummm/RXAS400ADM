package com.rxas400adm.operation.domain;

import java.util.Map;
import java.util.Set;

public enum OperationStatus {
    REQUESTED,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING,
    PARTIAL_SUCCESS,
    CANCELLED;

    private static final Map<OperationStatus, Set<OperationStatus>> TRANSITIONS = Map.of(
        REQUESTED, Set.of(RUNNING, CANCELLED),
        RUNNING, Set.of(SUCCESS, FAILED, RETRYING, PARTIAL_SUCCESS),
        FAILED, Set.of(RETRYING, CANCELLED),
        RETRYING, Set.of(RUNNING, CANCELLED),
        PARTIAL_SUCCESS, Set.of(RETRYING, CANCELLED),
        SUCCESS, Set.of(),
        CANCELLED, Set.of()
    );

    public boolean canTransitionTo(OperationStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}