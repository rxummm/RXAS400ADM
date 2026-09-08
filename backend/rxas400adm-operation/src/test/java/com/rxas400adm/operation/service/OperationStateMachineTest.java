package com.rxas400adm.operation.service;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.OperationStatus;
import com.rxas400adm.operation.mapper.OperationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationStateMachineTest {

    @Mock
    private OperationMapper operationMapper;

    @InjectMocks
    private OperationStateMachine stateMachine;

    private Operation createOp(String status, int version) {
        Operation op = new Operation();
        op.setId(1L);
        op.setStatus(status);
        op.setVersion(version);
        op.setRetryCount(0);
        op.setMaxRetry(3);
        return op;
    }

    @Test
    void shouldAdvanceState() {
        Operation op = createOp("REQUESTED", 1);
        when(operationMapper.casUpdateStatus(1L, "RUNNING", 1)).thenReturn(1);

        boolean result = stateMachine.advance(op, OperationStatus.RUNNING);

        assertTrue(result);
        assertEquals("RUNNING", op.getStatus());
        assertEquals(2, op.getVersion());
    }

    @Test
    void shouldRejectInvalidTransition() {
        Operation op = createOp("SUCCESS", 2);

        assertThrows(BusinessException.class, () ->
            stateMachine.advance(op, OperationStatus.RUNNING));
    }

    @Test
    void shouldReturnFalseOnCASConflict() {
        Operation op = createOp("REQUESTED", 1);
        when(operationMapper.casUpdateStatus(1L, "RUNNING", 1)).thenReturn(0);

        boolean result = stateMachine.advance(op, OperationStatus.RUNNING);

        assertFalse(result);
    }

    @Test
    void shouldMarkFailed() {
        Operation op = createOp("RUNNING", 2);

        stateMachine.markFailed(op, "TIMEOUT", "Timed out");

        verify(operationMapper).updateStatus(1L, "FAILED", "TIMEOUT", "Timed out", 2);
        assertEquals("FAILED", op.getStatus());
    }

    @Test
    void shouldRetryWhenUnderLimit() {
        Operation op = createOp("FAILED", 3);
        op.setRetryCount(1);

        boolean result = stateMachine.markRetrying(op);

        assertTrue(result);
        verify(operationMapper).incrementRetry(1L);
        assertEquals(2, op.getRetryCount());
    }

    @Test
    void shouldRejectRetryWhenExceeded() {
        Operation op = createOp("FAILED", 3);
        op.setRetryCount(3);

        boolean result = stateMachine.markRetrying(op);

        assertFalse(result);
        verify(operationMapper, never()).incrementRetry(anyLong());
    }
}