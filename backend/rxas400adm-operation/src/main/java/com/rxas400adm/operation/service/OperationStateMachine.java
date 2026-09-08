package com.rxas400adm.operation.service;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.OperationStatus;
import com.rxas400adm.operation.mapper.OperationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationStateMachine {

    private final OperationMapper operationMapper;

    public boolean advance(Operation op, OperationStatus targetStatus) {
        OperationStatus current = OperationStatus.valueOf(op.getStatus());
        if (!current.canTransitionTo(targetStatus)) {
            log.warn("Illegal state transition: {} -> {} (opId={})", op.getStatus(), targetStatus, op.getId());
            throw new BusinessException(ErrorCode.OPERATION_STATUS_CONFLICT,
                "Illegal state transition: " + op.getStatus() + " -> " + targetStatus);
        }
        int affected = operationMapper.casUpdateStatus(op.getId(), targetStatus.name(), op.getVersion());
        if (affected == 0) {
            log.warn("Operation CAS conflict: id={}, currentVersion={}", op.getId(), op.getVersion());
            return false;
        }
        op.setStatus(targetStatus.name());
        op.setVersion(op.getVersion() + 1);
        return true;
    }

    public void markFailed(Operation op, String errorCode, String errorMessage) {
        operationMapper.updateStatus(op.getId(), OperationStatus.FAILED.name(),
            errorCode, errorMessage, op.getVersion());
        op.setStatus(OperationStatus.FAILED.name());
        op.setErrorCode(errorCode);
        op.setErrorMessage(errorMessage);
    }

    public boolean markRetrying(Operation op) {
        if (op.getRetryCount() >= op.getMaxRetry()) {
            log.warn("Operation reached max retry: id={}, retry={}/{}", op.getId(), op.getRetryCount(), op.getMaxRetry());
            return false;
        }
        operationMapper.incrementRetry(op.getId());
        op.setRetryCount(op.getRetryCount() + 1);
        return true;
    }
}