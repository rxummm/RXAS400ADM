package com.rxas400adm.operation.policy;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.operation.annotation.IbmiOperation;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.RiskLevel;
import com.rxas400adm.operation.executor.OperationExecutor;
import com.rxas400adm.operation.service.OperationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationPolicyEngine {

    private final OperationRegistry registry;
    private final RiskPolicy riskPolicy;
    private final ConfirmationPolicy confirmationPolicy;

    public void check(Operation op, String confirmationToken) {
        OperationExecutor executor = registry.get(op.getOperationType());
        if (executor == null) {
            throw new BusinessException(ErrorCode.OPERATION_TYPE_UNKNOWN, "Unknown operation type: " + op.getOperationType());
        }

        IbmiOperation annotation = executor.getClass().getAnnotation(IbmiOperation.class);
        if (annotation == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Operation missing @IbmiOperation annotation");
        }

        if (!annotation.requiredPermission().isEmpty()) {
            riskPolicy.checkPermission(annotation.requiredPermission());
        }

        riskPolicy.checkRiskLevel(annotation.riskLevel(), op);

        confirmationPolicy.checkConfirmation(annotation.riskLevel(), confirmationToken);
    }
}