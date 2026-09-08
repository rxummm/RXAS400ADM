package com.rxas400adm.operation.gateway;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.policy.OperationPolicyEngine;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class IbmiGateway {

    private final AS400ClientProvider clientProvider;
    private final OperationPolicyEngine policyEngine;

    @CircuitBreaker(name = "ibmiGateway", fallbackMethod = "fallback")
    public <T> T execute(Operation op, Function<AS400Client, T> action, String confirmationToken) {
        policyEngine.check(op, confirmationToken);

        AS400Client client = clientProvider.current();

        log.info("Gateway executing: op={}, type={}, target={}", op.getId(), op.getOperationType(), op.getTargetName());
        long start = System.currentTimeMillis();
        try {
            T result = action.apply(client);
            log.info("Gateway succeeded: op={}, duration={}ms", op.getId(), System.currentTimeMillis() - start);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gateway failed: op={}, error={}", op.getId(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.AS400_CONNECTION_FAILED,
                "IBM i operation failed: " + e.getMessage());
        }
    }

    public <T> T fallback(Operation op, Function<AS400Client, T> action, String confirmationToken, Throwable t) {
        log.error("Gateway fallback triggered: op={}, error={}", op.getId(), t.getMessage(), t);
        throw new BusinessException(ErrorCode.AS400_CONNECTION_FAILED,
            "IBM i circuit breaker open, operation rejected: " + t.getMessage());
    }
}