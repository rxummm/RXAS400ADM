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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@IbmiOperation(
    code = "IFS_WRITE",
    riskLevel = RiskLevel.WRITE,
    requiredPermission = "IFS_MANAGE",
    description = "Write file to IBM i IFS"
)
@Component
@RequiredArgsConstructor
public class IfsWriteExecutor implements OperationExecutor {

    private static final Pattern SAFE_IFS_PATH = Pattern.compile("^/QOpenSys/[A-Za-z0-9_./-]+$");

    private final AS400ClientProvider clientProvider;

    @Override
    public List<String> defineSteps() {
        return List.of("MKDIR", "WRITE_FILE", "VERIFY_FILE");
    }

    @Override
    public StepResult executeStep(Operation op, String stepCode) {
        AS400Client client = clientProvider.current();
        long start = System.currentTimeMillis();

        return switch (stepCode) {
            case "MKDIR" -> {
                String path = OperationJsonUtil.extractField(op, "path");
                if (path == null || path.isBlank()) {
                    yield StepResult.failed("INVALID_PARAM", "Missing parameter", "path is required",
                        System.currentTimeMillis() - start);
                }
                if (path.contains("..") || !SAFE_IFS_PATH.matcher(path).matches()) {
                    yield StepResult.failed("INVALID_PARAM", "Invalid path", "Path must be under /QOpenSys/ and not contain ..",
                        System.currentTimeMillis() - start);
                }
                String parent = path.substring(0, path.lastIndexOf('/'));
                boolean ok = client.mkdirIfs(parent);
                yield ok
                    ? StepResult.success("OK", "Directory ensured", System.currentTimeMillis() - start)
                    : StepResult.failed("MKDIR_FAILED", "Failed to create directory", parent,
                        System.currentTimeMillis() - start);
            }
            case "WRITE_FILE" -> {
                String path = OperationJsonUtil.extractField(op, "path");
                String content = OperationJsonUtil.extractField(op, "content");
                if (path == null || path.isBlank()) {
                    yield StepResult.failed("INVALID_PARAM", "Missing parameter", "path is required",
                        System.currentTimeMillis() - start);
                }
                if (path.contains("..") || !SAFE_IFS_PATH.matcher(path).matches()) {
                    yield StepResult.failed("INVALID_PARAM", "Invalid path", "Path must be under /QOpenSys/ and not contain ..",
                        System.currentTimeMillis() - start);
                }
                boolean ok = client.writeIfsFile(path, content != null ? content : "");
                yield ok
                    ? StepResult.success("OK", "File written", System.currentTimeMillis() - start)
                    : StepResult.failed("WRITE_FAILED", "Failed to write file", path,
                        System.currentTimeMillis() - start);
            }
            case "VERIFY_FILE" -> {
                String path = OperationJsonUtil.extractField(op, "path");
                String content = client.readIfsFile(path);
                if (content != null) {
                    yield StepResult.success("OK", "File verified", System.currentTimeMillis() - start);
                } else {
                    yield StepResult.failed("VERIFY_FAILED", "File not found after write", path,
                        System.currentTimeMillis() - start);
                }
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown step: " + stepCode);
        };
    }
}