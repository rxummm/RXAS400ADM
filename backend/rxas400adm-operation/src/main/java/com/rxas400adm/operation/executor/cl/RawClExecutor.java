package com.rxas400adm.operation.executor.cl;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.security.DangerousClCommandValidator;
import com.rxas400adm.operation.annotation.IbmiOperation;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.RiskLevel;
import com.rxas400adm.operation.domain.StepResult;
import com.rxas400adm.operation.executor.OperationExecutor;
import com.rxas400adm.operation.policy.CommandPolicy;
import com.rxas400adm.operation.util.OperationJsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@IbmiOperation(
    code = "RAW_CL",
    riskLevel = RiskLevel.BREAK_GLASS,
    requiredPermission = "BREAK_GLASS_EXECUTE",
    description = "Execute raw CL command (break-glass)"
)
@Component
@RequiredArgsConstructor
public class RawClExecutor implements OperationExecutor {

    private final AS400ClientProvider clientProvider;
    private final DangerousClCommandValidator clValidator;
    private final CommandPolicy commandPolicy;

    @Override
    public List<String> defineSteps() {
        return List.of("VALIDATE_COMMAND", "EXECUTE_COMMAND", "VERIFY_COMMAND");
    }

    @Override
    public StepResult executeStep(Operation op, String stepCode) {
        AS400Client client = clientProvider.current();
        long start = System.currentTimeMillis();

        return switch (stepCode) {
            case "VALIDATE_COMMAND" -> {
                String command = OperationJsonUtil.extractField(op, "command");
                if (command == null || command.isBlank()) {
                    yield StepResult.failed("INVALID_PARAM", "Missing parameter", "command is required",
                        System.currentTimeMillis() - start);
                }
                try {
                    clValidator.assertAllowed(command);
                    commandPolicy.validateClCommand(command);
                    yield StepResult.success("OK", "Command validation passed", System.currentTimeMillis() - start);
                } catch (Exception e) {
                    yield StepResult.failed("COMMAND_BLOCKED", e.getMessage(), e.getMessage(),
                        System.currentTimeMillis() - start);
                }
            }
            case "EXECUTE_COMMAND" -> {
                String command = OperationJsonUtil.extractField(op, "command");
                var result = client.execute(command);
                yield result.success()
                    ? StepResult.success(result.message(), "Command executed", System.currentTimeMillis() - start)
                    : StepResult.failed("CPF0001", result.message(), result.message(), System.currentTimeMillis() - start);
            }
            case "VERIFY_COMMAND" -> {
                yield StepResult.success("OK", "Raw CL command verification requires manual review",
                    System.currentTimeMillis() - start);
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown step: " + stepCode);
        };
    }

    @Override
    public StepResult verifyStep(Operation op, String stepCode) {
        if ("VERIFY_COMMAND".equals(stepCode)) {
            return executeStep(op, "VERIFY_COMMAND");
        }
        return null;
    }
}