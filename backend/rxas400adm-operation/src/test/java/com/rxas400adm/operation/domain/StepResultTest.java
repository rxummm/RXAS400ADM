package com.rxas400adm.operation.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StepResultTest {

    @Test
    void shouldCreateSuccessResult() {
        StepResult result = StepResult.success("OK", "Operation completed", 1000L);
        assertEquals(StepResult.StepStatus.SUCCESS, result.status());
        assertEquals("OK", result.ibmiReturnCode());
        assertEquals("Operation completed", result.ibmiMessage());
        assertEquals(1000L, result.durationMs());
    }

    @Test
    void shouldCreateFailedResult() {
        StepResult result = StepResult.failed("CPF0001", "Error occurred", "Detail", 500L);
        assertEquals(StepResult.StepStatus.FAILED, result.status());
        assertEquals("CPF0001", result.ibmiReturnCode());
        assertEquals("Error occurred", result.ibmiMessage());
        assertEquals("Detail", result.errorDetail());
    }
}