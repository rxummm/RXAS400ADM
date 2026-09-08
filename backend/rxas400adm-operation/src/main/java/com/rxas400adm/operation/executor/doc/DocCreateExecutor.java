package com.rxas400adm.operation.executor.doc;

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
    code = "DOC_CREATE",
    riskLevel = RiskLevel.WRITE,
    requiredPermission = "DOC_MANAGE",
    description = "Create document and write to IBM i IFS"
)
@Component
@RequiredArgsConstructor
public class DocCreateExecutor implements OperationExecutor {

    private static final Pattern SAFE_DOC_ID = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern SAFE_IFS_PATH = Pattern.compile("^/QOpenSys/[A-Za-z0-9_./-]+$");

    private final AS400ClientProvider clientProvider;

    @Override
    public List<String> defineSteps() {
        return List.of("CREATE_DB_RECORD", "WRITE_TO_IFS", "VERIFY_CREATED");
    }

    @Override
    public StepResult executeStep(Operation op, String stepCode) {
        AS400Client client = clientProvider.current();
        long start = System.currentTimeMillis();

        return switch (stepCode) {
            case "CREATE_DB_RECORD" -> {
                String docId = OperationJsonUtil.extractField(op, "docId");
                String title = OperationJsonUtil.extractField(op, "title");

                if (docId == null || docId.isBlank()) {
                    yield StepResult.failed("INVALID_PARAM", "Missing parameter", "docId is required",
                        System.currentTimeMillis() - start);
                }
                if (!SAFE_DOC_ID.matcher(docId).matches()) {
                    yield StepResult.failed("INVALID_PARAM", "Invalid docId", "docId must contain only alphanumeric, dash, or underscore",
                        System.currentTimeMillis() - start);
                }
                if (title == null || title.isBlank()) {
                    yield StepResult.failed("INVALID_PARAM", "Missing parameter", "title is required",
                        System.currentTimeMillis() - start);
                }
                yield StepResult.success("OK", "DB record creation delegated to service layer",
                    System.currentTimeMillis() - start);
            }
            case "WRITE_TO_IFS" -> {
                String docId = OperationJsonUtil.extractField(op, "docId");
                String content = OperationJsonUtil.extractField(op, "content");
                String fileType = OperationJsonUtil.extractField(op, "fileType");

                if ("PDF".equals(fileType) || "IMAGE".equals(fileType)) {
                    yield StepResult.success("SKIP", "Binary doc, IFS write handled by upload",
                        System.currentTimeMillis() - start);
                }

                String ext = fileType != null ? fileType.toLowerCase() : "md";
                String path = "/QOpenSys/rxas400/docs/" + docId + "/doc." + ext;
                if (!SAFE_IFS_PATH.matcher(path).matches()) {
                    yield StepResult.failed("INVALID_PARAM", "Invalid path", "Constructed IFS path is invalid",
                        System.currentTimeMillis() - start);
                }
                boolean ok = client.writeIfsFile(path, content != null ? content : "");
                yield ok
                    ? StepResult.success("OK", "Written to IFS: " + path, System.currentTimeMillis() - start)
                    : StepResult.failed("WRITE_FAILED", "Failed to write to IFS", path,
                        System.currentTimeMillis() - start);
            }
            case "VERIFY_CREATED" -> {
                String docId = OperationJsonUtil.extractField(op, "docId");
                String fileType = OperationJsonUtil.extractField(op, "fileType");

                if ("PDF".equals(fileType) || "IMAGE".equals(fileType)) {
                    yield StepResult.success("SKIP", "Binary doc verification skipped",
                        System.currentTimeMillis() - start);
                }

                String ext = fileType != null ? fileType.toLowerCase() : "md";
                String path = "/QOpenSys/rxas400/docs/" + docId + "/doc." + ext;
                String content = client.readIfsFile(path);
                if (content != null) {
                    yield StepResult.success("OK", "Document verified in IFS", System.currentTimeMillis() - start);
                } else {
                    yield StepResult.failed("VERIFY_FAILED", "File not found after creation", path,
                        System.currentTimeMillis() - start);
                }
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown step: " + stepCode);
        };
    }

    @Override
    public StepResult verifyStep(Operation op, String stepCode) {
        if ("VERIFY_CREATED".equals(stepCode)) {
            return executeStep(op, "VERIFY_CREATED");
        }
        return null;
    }
}