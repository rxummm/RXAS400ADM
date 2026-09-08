package com.rxas400adm.operation.executor.spool;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.SpoolRow;
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
    code = "SPOOL_DELETE",
    riskLevel = RiskLevel.DESTRUCTIVE,
    requiredPermission = "JOB_VIEW",
    description = "Delete spool file from IBM i output queue"
)
@Component
@RequiredArgsConstructor
public class SpoolDeleteExecutor implements OperationExecutor {

    private final AS400ClientProvider clientProvider;

    @Override
    public List<String> defineSteps() {
        return List.of("VALIDATE_PARAMS", "DELETE_SPOOL", "VERIFY_DELETED");
    }

    @Override
    public StepResult executeStep(Operation op, String stepCode) {
        AS400Client client = clientProvider.current();
        long start = System.currentTimeMillis();

        return switch (stepCode) {
            case "VALIDATE_PARAMS" -> {
                String jobName = OperationJsonUtil.extractField(op, "jobName");
                String jobUser = OperationJsonUtil.extractField(op, "jobUser");
                String jobNumber = OperationJsonUtil.extractField(op, "jobNumber");
                String spoolName = OperationJsonUtil.extractField(op, "spoolName");
                String outputQueue = OperationJsonUtil.extractField(op, "outputQueue");

                if (jobName == null || jobName.isBlank()) {
                    yield StepResult.failed("INVALID_PARAM", "Missing parameter", "jobName is required",
                        System.currentTimeMillis() - start);
                }
                if (spoolName == null || spoolName.isBlank()) {
                    yield StepResult.failed("INVALID_PARAM", "Missing parameter", "spoolName is required",
                        System.currentTimeMillis() - start);
                }
                if (!As400Identifiers.IDENTIFIER.matcher(jobName).matches()) {
                    yield StepResult.failed("INVALID_PARAM", "Invalid identifier", "jobName contains invalid characters",
                        System.currentTimeMillis() - start);
                }
                if (jobUser != null && !jobUser.isBlank()
                    && !As400Identifiers.IDENTIFIER.matcher(jobUser).matches()) {
                    yield StepResult.failed("INVALID_PARAM", "Invalid identifier", "jobUser contains invalid characters",
                        System.currentTimeMillis() - start);
                }
                if (jobNumber != null && !jobNumber.isBlank()
                    && !As400Identifiers.JOB_NUMBER.matcher(jobNumber).matches()) {
                    yield StepResult.failed("INVALID_PARAM", "Invalid job number", "jobNumber must be 1-6 digits",
                        System.currentTimeMillis() - start);
                }
                if (!As400Identifiers.IDENTIFIER.matcher(spoolName).matches()) {
                    yield StepResult.failed("INVALID_PARAM", "Invalid identifier", "spoolName contains invalid characters",
                        System.currentTimeMillis() - start);
                }
                if (outputQueue != null && !outputQueue.isBlank()
                    && !As400Identifiers.IDENTIFIER.matcher(outputQueue).matches()
                    && !"*SELECT".equals(outputQueue)) {
                    yield StepResult.failed("INVALID_PARAM", "Invalid output queue", "outputQueue contains invalid characters",
                        System.currentTimeMillis() - start);
                }
                yield StepResult.success("OK", "Parameters validated", System.currentTimeMillis() - start);
            }
            case "DELETE_SPOOL" -> {
                String jobName = OperationJsonUtil.extractField(op, "jobName");
                String jobUser = OperationJsonUtil.extractField(op, "jobUser");
                String jobNumber = OperationJsonUtil.extractField(op, "jobNumber");
                String spoolName = OperationJsonUtil.extractField(op, "spoolName");
                String outputQueue = OperationJsonUtil.extractField(op, "outputQueue");

                var result = client.deleteSpoolFile(jobName, jobUser, jobNumber, spoolName, outputQueue);
                yield result.success()
                    ? StepResult.success(result.message(), "Spool file deleted", System.currentTimeMillis() - start)
                    : StepResult.failed("CPF0001", result.message(), result.message(),
                        System.currentTimeMillis() - start);
            }
            case "VERIFY_DELETED" -> {
                String jobName = OperationJsonUtil.extractField(op, "jobName");
                String jobUser = OperationJsonUtil.extractField(op, "jobUser");
                String jobNumber = OperationJsonUtil.extractField(op, "jobNumber");
                String spoolName = OperationJsonUtil.extractField(op, "spoolName");

                List<SpoolRow> spools = client.listSpoolFiles(jobName, jobUser, jobNumber);
                boolean found = spools.stream()
                    .anyMatch(s -> spoolName.equalsIgnoreCase(s.name()));

                if (!found) {
                    yield StepResult.success("OK", "Spool file no longer exists", System.currentTimeMillis() - start);
                } else {
                    yield StepResult.failed("SPOOL_STILL_EXISTS", "Spool file still exists",
                        "Spool " + spoolName + " still present in output queue",
                        System.currentTimeMillis() - start);
                }
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown step: " + stepCode);
        };
    }
}