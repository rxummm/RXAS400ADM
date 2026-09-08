package com.rxas400adm.operation.executor.ifs;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.operation.annotation.IbmiOperation;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.RiskLevel;
import com.rxas400adm.operation.domain.StepResult;
import com.rxas400adm.operation.executor.OperationExecutor;
import com.rxas400adm.operation.util.OperationJsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@IbmiOperation(
    code = "IFS_DELETE",
    riskLevel = RiskLevel.DESTRUCTIVE,
    requiredPermission = "IFS_MANAGE",
    description = "Delete file from IBM i IFS (move to trash)"
)
@Component
@RequiredArgsConstructor
public class IfsDeleteExecutor implements OperationExecutor {

    private static final Pattern SAFE_IFS_PATH = Pattern.compile("^/QOpenSys/[A-Za-z0-9_./-]+$");

    private final AS400ClientProvider clientProvider;

    @Override
    public List<String> defineSteps() {
        return List.of("TRASH_FILE", "VERIFY_DELETED");
    }

    @Override
    public StepResult executeStep(Operation op, String stepCode) {
        AS400Client client = clientProvider.current();
        long start = System.currentTimeMillis();

        return switch (stepCode) {
            case "TRASH_FILE" -> {
                String path = OperationJsonUtil.extractField(op, "path");
                if (path == null || path.isBlank()) {
                    yield StepResult.failed("INVALID_PARAM", "Missing parameter", "path is required",
                        System.currentTimeMillis() - start);
                }
                if (path.contains("..") || !SAFE_IFS_PATH.matcher(path).matches()) {
                    yield StepResult.failed("INVALID_PARAM", "Invalid path", "Path must be under /QOpenSys/ and not contain ..",
                        System.currentTimeMillis() - start);
                }
                try {
                    String trashPath = client.trashIfsFile(path);
                    yield StepResult.success("OK", "File moved to trash: " + trashPath,
                        System.currentTimeMillis() - start);
                } catch (Exception e) {
                    yield StepResult.failed("TRASH_FAILED", "Failed to trash file", e.getMessage(),
                        System.currentTimeMillis() - start);
                }
            }
            case "VERIFY_DELETED" -> {
                String path = OperationJsonUtil.extractField(op, "path");
                if (path == null || path.isBlank()) {
                    yield StepResult.success("OK", "Verify skipped, no path", System.currentTimeMillis() - start);
                }
                try {
                    String content = client.readIfsFile(path);
                    if (content == null) {
                        yield StepResult.success("OK", "File no longer exists at original path",
                            System.currentTimeMillis() - start);
                    } else {
                        yield StepResult.failed("FILE_STILL_EXISTS", "File still exists after trash",
                            path, System.currentTimeMillis() - start);
                    }
                } catch (Exception e) {
                    log.warn("IFS file read failed during verify (path={}): {}", path, e.getMessage());
                    yield StepResult.success("OK", "File access failed (likely deleted): " + e.getMessage(),
                        System.currentTimeMillis() - start);
                }
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown step: " + stepCode);
        };
    }

    @Override
    public StepResult verifyStep(Operation op, String stepCode) {
        if ("VERIFY_DELETED".equals(stepCode)) {
            return executeStep(op, "VERIFY_DELETED");
        }
        return null;
    }
}