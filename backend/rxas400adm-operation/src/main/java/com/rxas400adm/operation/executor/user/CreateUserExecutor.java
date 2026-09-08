package com.rxas400adm.operation.executor.user;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.common.constants.As400Identifiers;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.operation.annotation.IbmiOperation;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.RiskLevel;
import com.rxas400adm.operation.domain.StepResult;
import com.rxas400adm.operation.executor.OperationExecutor;
import com.rxas400adm.operation.util.OperationJsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@IbmiOperation(
    code = "USER_CREATE",
    riskLevel = RiskLevel.WRITE,
    requiredPermission = "USER_PROFILE_CREATE",
    description = "Create IBM i User Profile"
)
@Component
@RequiredArgsConstructor
public class CreateUserExecutor implements OperationExecutor {

    private final AS400ClientProvider clientProvider;

    @Override
    public List<String> defineSteps() {
        return List.of("CREATE_USER", "SET_GROUP", "SET_AUTHORITY", "VERIFY");
    }

    @Override
    public StepResult executeStep(Operation op, String stepCode) {
        AS400Client client = clientProvider.current();
        long start = System.currentTimeMillis();

        return switch (stepCode) {
            case "CREATE_USER" -> {
                String userName = validateIdentifier(op.getTargetName(), "userName");
                // 使用参数化查询防止 SQL 注入
                var existing = client.queryList(
                    "SELECT USER_PROFILE_NAME FROM TABLE(QSYS2.USER_INFO(?)) X", userName);
                if (!existing.isEmpty()) {
                    yield StepResult.success("CPF2217", "User already exists, treated as success",
                        System.currentTimeMillis() - start);
                }
                String cmd = "CRTUSRPRF USRPRF(" + userName + ") PASSWORD(*NONE) STATUS(*ENABLED)";
                var result = client.execute(cmd);
                yield result.success()
                    ? StepResult.success(result.message(), "User created", System.currentTimeMillis() - start)
                    : StepResult.failed("CPF0001", result.message(), result.message(), System.currentTimeMillis() - start);
            }
            case "SET_GROUP" -> {
                String userName = validateIdentifier(op.getTargetName(), "userName");
                String group = OperationJsonUtil.extractField(op, "group");
                if (group == null || group.isBlank()) {
                    yield StepResult.success("N/A", "No group specified, skipped", System.currentTimeMillis() - start);
                }
                validateIdentifier(group, "group");
                String cmd = "CHGUSRPRF USRPRF(" + userName + ") GRPPRF(" + group + ")";
                var result = client.execute(cmd);
                yield result.success()
                    ? StepResult.success(result.message(), "Group set", System.currentTimeMillis() - start)
                    : StepResult.failed("CPF0001", result.message(), result.message(), System.currentTimeMillis() - start);
            }
            case "SET_AUTHORITY" -> {
                String userName = validateIdentifier(op.getTargetName(), "userName");
                String authority = OperationJsonUtil.extractField(op, "authority");
                if (authority == null || authority.isBlank()) {
                    yield StepResult.success("N/A", "No authority specified, skipped", System.currentTimeMillis() - start);
                }
                validateIdentifier(authority, "authority");
                String cmd = "CHGUSRPRF USRPRF(" + userName + ") SPCAUT(" + authority + ")";
                var result = client.execute(cmd);
                yield result.success()
                    ? StepResult.success(result.message(), "Authority set", System.currentTimeMillis() - start)
                    : StepResult.failed("CPF0001", result.message(), result.message(), System.currentTimeMillis() - start);
            }
            case "VERIFY" -> {
                String userName = validateIdentifier(op.getTargetName(), "userName");
                var users = client.queryList(
                    "SELECT USER_PROFILE_NAME, USER_CLASS_NAME, USER_STATUS " +
                    "FROM TABLE(QSYS2.USER_INFO(?)) X", userName);
                if (users.isEmpty()) {
                    yield StepResult.failed("CPF2217", "Verification failed", "User not found",
                        System.currentTimeMillis() - start);
                }
                yield StepResult.success("OK", "Verification passed", System.currentTimeMillis() - start);
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown step: " + stepCode);
        };
    }

    @Override
    public StepResult verifyStep(Operation op, String stepCode) {
        if ("VERIFY".equals(stepCode)) {
            return executeStep(op, "VERIFY");
        }
        return null;
    }

    private String validateIdentifier(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " is required");
        }
        if (!As400Identifiers.IDENTIFIER.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                fieldName + " contains invalid characters: " + value);
        }
        return value;
    }
}