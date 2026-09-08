package com.rxas400adm.operation.util;

import com.rxas400adm.operation.domain.Operation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperationJsonUtilTest {

    private Operation createOp(String json) {
        Operation op = new Operation();
        op.setRequestData(json);
        return op;
    }

    @Test
    void shouldExtractField() {
        Operation op = createOp("{\"name\":\"test\",\"count\":123}");
        assertEquals("test", OperationJsonUtil.extractField(op, "name"));
    }

    @Test
    void shouldReturnNullForMissingField() {
        Operation op = createOp("{\"name\":\"test\"}");
        assertNull(OperationJsonUtil.extractField(op, "missing"));
    }

    @Test
    void shouldHandleNullJson() {
        Operation op = createOp(null);
        assertNull(OperationJsonUtil.extractField(op, "name"));
    }

    @Test
    void shouldHandleInvalidJson() {
        Operation op = createOp("not json");
        assertNull(OperationJsonUtil.extractField(op, "name"));
    }
}