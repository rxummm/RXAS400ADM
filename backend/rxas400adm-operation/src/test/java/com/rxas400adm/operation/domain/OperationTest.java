package com.rxas400adm.operation.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperationTest {

    @Test
    void shouldCreateOperationWithDefaults() {
        Operation op = new Operation();
        assertNull(op.getRetryCount());
        assertNull(op.getStartedAt());
        assertNull(op.getCompletedAt());
    }

    @Test
    void shouldSupportIdempotencyKey() {
        Operation op = new Operation();
        op.setIdempotencyKey("test-idempotency-key");
        assertEquals("test-idempotency-key", op.getIdempotencyKey());
    }

    @Test
    void shouldTrackRequestData() {
        Operation op = new Operation();
        op.setRequestData("{\"name\":\"test\"}");
        assertTrue(op.getRequestData().contains("test"));
    }

    @Test
    void shouldTrackVersion() {
        Operation op = new Operation();
        op.setVersion(1);
        assertEquals(1, op.getVersion());
        op.setVersion(2);
        assertEquals(2, op.getVersion());
    }
}