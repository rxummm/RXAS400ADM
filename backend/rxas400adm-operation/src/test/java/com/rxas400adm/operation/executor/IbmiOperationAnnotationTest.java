package com.rxas400adm.operation.executor;

import com.rxas400adm.operation.annotation.IbmiOperation;
import com.rxas400adm.operation.domain.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IbmiOperationAnnotationTest {

    @IbmiOperation(
        code = "TEST_OP",
        riskLevel = RiskLevel.READ,
        requiredPermission = "TEST_VIEW",
        description = "Test operation"
    )
    static class TestOperation {}

    @Test
    void shouldHaveAnnotation() {
        assertTrue(TestOperation.class.isAnnotationPresent(IbmiOperation.class));
    }

    @Test
    void shouldExtractCode() {
        IbmiOperation annotation = TestOperation.class.getAnnotation(IbmiOperation.class);
        assertEquals("TEST_OP", annotation.code());
    }

    @Test
    void shouldExtractRiskLevel() {
        IbmiOperation annotation = TestOperation.class.getAnnotation(IbmiOperation.class);
        assertEquals(RiskLevel.READ, annotation.riskLevel());
    }

    @Test
    void shouldExtractPermission() {
        IbmiOperation annotation = TestOperation.class.getAnnotation(IbmiOperation.class);
        assertEquals("TEST_VIEW", annotation.requiredPermission());
    }
}