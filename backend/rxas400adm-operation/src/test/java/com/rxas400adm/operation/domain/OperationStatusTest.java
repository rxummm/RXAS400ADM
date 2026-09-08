package com.rxas400adm.operation.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperationStatusTest {

    @Test
    void shouldAllowValidTransitions() {
        assertTrue(OperationStatus.REQUESTED.canTransitionTo(OperationStatus.RUNNING));
        assertTrue(OperationStatus.RUNNING.canTransitionTo(OperationStatus.SUCCESS));
        assertTrue(OperationStatus.RUNNING.canTransitionTo(OperationStatus.FAILED));
        assertTrue(OperationStatus.FAILED.canTransitionTo(OperationStatus.RETRYING));
        assertTrue(OperationStatus.RETRYING.canTransitionTo(OperationStatus.RUNNING));
        assertTrue(OperationStatus.REQUESTED.canTransitionTo(OperationStatus.CANCELLED));
    }

    @Test
    void shouldRejectInvalidTransitions() {
        assertFalse(OperationStatus.SUCCESS.canTransitionTo(OperationStatus.RUNNING));
        assertFalse(OperationStatus.FAILED.canTransitionTo(OperationStatus.SUCCESS));
        assertFalse(OperationStatus.CANCELLED.canTransitionTo(OperationStatus.RUNNING));
    }

    @Test
    void shouldHaveTerminalStates() {
        assertFalse(OperationStatus.SUCCESS.canTransitionTo(OperationStatus.RUNNING));
        assertFalse(OperationStatus.CANCELLED.canTransitionTo(OperationStatus.RUNNING));
        assertFalse(OperationStatus.SUCCESS.canTransitionTo(OperationStatus.FAILED));
        assertFalse(OperationStatus.CANCELLED.canTransitionTo(OperationStatus.FAILED));
    }

    @Test
    void shouldHaveSevenValues() {
        assertEquals(7, OperationStatus.values().length);
    }
}