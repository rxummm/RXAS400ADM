package com.rxas400adm.operation.service;

import com.rxas400adm.operation.annotation.IbmiOperation;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.RiskLevel;
import com.rxas400adm.operation.domain.StepResult;
import com.rxas400adm.operation.executor.OperationExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = { com.rxas400adm.operation.config.OperationAutoConfiguration.class, OperationCompositeConfig.class })
class OperationRegistryIntegrationTest {

    @Autowired
    private OperationRegistry registry;

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private SimpMessagingTemplate simpMessagingTemplate;

    @Test
    void shouldRegisterKnownExecutors() {
        assertNotNull(registry);
        var all = registry.getAll();
        assertFalse(all.isEmpty(), "Expected at least one registered operation executor");

        for (var entry : all.entrySet()) {
            String code = entry.getKey();
            OperationExecutor executor = entry.getValue();
            assertNotNull(code);
            assertNotNull(executor);

            IbmiOperation annotation = executor.getClass().getAnnotation(IbmiOperation.class);
            assertNotNull(annotation, "Executor should carry @IbmiOperation: " + executor.getClass().getName());
            assertEquals(code, annotation.code());
        }

        assertRegistered("USER_CREATE");
        assertRegistered("END_JOB");
        assertRegistered("RAW_CL");
    }

    private void assertRegistered(String code) {
        assertTrue(registry.get(code) != null, "Expected operation code to be registered: " + code);
    }

    @Test
    void annotationContractShouldBeEnforceableForCustomExecutor() {
        StubCreateExecutor bean = new StubCreateExecutor();
        IbmiOperation annotation = bean.getClass().getAnnotation(IbmiOperation.class);
        assertNotNull(annotation);
        assertEquals("STUB_CREATE", annotation.code());
        assertEquals(RiskLevel.WRITE, annotation.riskLevel());
        assertEquals("USER_CREATE", annotation.requiredPermission());
    }

    @IbmiOperation(code = "STUB_CREATE", riskLevel = RiskLevel.WRITE, requiredPermission = "USER_CREATE")
    public static class StubCreateExecutor implements OperationExecutor {

        @Override
        public List<String> defineSteps() {
            return List.of("STUB_STEP");
        }

        @Override
        public StepResult executeStep(Operation op, String stepCode) {
            return StepResult.success("OK", "stub", 0);
        }
    }
}
