package com.rxas400adm.operation.executor.job;

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
import java.util.regex.Pattern;

@IbmiOperation(
    code = "END_JOB",
    riskLevel = RiskLevel.DESTRUCTIVE,
    requiredPermission = "JOB_END",
    description = "End IBM i Job"
)
@Component
@RequiredArgsConstructor
public class EndJobExecutor implements OperationExecutor {

    private final AS400ClientProvider clientProvider;

    @Override
    public List<String> defineSteps() {
        return List.of("END_JOB", "VERIFY_JOB_ENDED");
    }

    @Override
    public StepResult executeStep(Operation op, String stepCode) {
        AS400Client client = clientProvider.current();
        long start = System.currentTimeMillis();

        return switch (stepCode) {
            case "END_JOB" -> {
                String jobName = validateJobParam(OperationJsonUtil.extractField(op, "jobName"), "jobName", As400Identifiers.IDENTIFIER);
                String jobUser = validateJobParam(OperationJsonUtil.extractField(op, "jobUser"), "jobUser", As400Identifiers.IDENTIFIER);
                String jobNumber = validateJobParam(OperationJsonUtil.extractField(op, "jobNumber"), "jobNumber", As400Identifiers.JOB_NUMBER);

                String jobKey = jobNumber + "/" + jobUser + "/" + jobName;
                String cmd = "ENDJOB JOB(" + jobKey + ") OPTION(*IMMED)";
                var result = client.execute(cmd);
                yield result.success()
                    ? StepResult.success(result.message(), "Job ended", System.currentTimeMillis() - start)
                    : StepResult.failed("CPF0001", result.message(), result.message(), System.currentTimeMillis() - start);
            }
            case "VERIFY_JOB_ENDED" -> {
                String jobName = validateJobParam(OperationJsonUtil.extractField(op, "jobName"), "jobName", As400Identifiers.IDENTIFIER);
                String jobUser = validateJobParam(OperationJsonUtil.extractField(op, "jobUser"), "jobUser", As400Identifiers.IDENTIFIER);
                String jobNumber = validateJobParam(OperationJsonUtil.extractField(op, "jobNumber"), "jobNumber", As400Identifiers.JOB_NUMBER);
                String jobKey = jobNumber + "/" + jobUser + "/" + jobName;

                // 使用参数化查询防止 SQL 注入
                var jobs = client.queryList(
                    "SELECT JOB_NAME FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) X " +
                    "WHERE JOB_NAME = ?", jobKey);

                if (jobs.isEmpty()) {
                    yield StepResult.success("OK", "Job no longer active", System.currentTimeMillis() - start);
                } else {
                    yield StepResult.failed("JOB_STILL_ACTIVE", "Job still active",
                        "Job " + jobKey + " is still running", System.currentTimeMillis() - start);
                }
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown step: " + stepCode);
        };
    }

    @Override
    public StepResult verifyStep(Operation op, String stepCode) {
        if ("VERIFY_JOB_ENDED".equals(stepCode)) {
            return executeStep(op, "VERIFY_JOB_ENDED");
        }
        return null;
    }

    /**
     * 校验 IBM i 参数值，防止 SQL 注入和 CL 命令注入。
     * 空值/空白/不匹配正则时返回 StepResult.failed 而非抛异常，
     * 使流程自然进入失败分支而非中断。
     */
    private String validateJobParam(String value, String fieldName, Pattern pattern) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " is required");
        }
        if (!pattern.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                fieldName + " contains invalid characters: " + value);
        }
        return value;
    }
}