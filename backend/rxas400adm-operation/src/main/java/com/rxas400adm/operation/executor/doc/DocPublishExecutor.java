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
    code = "DOC_PUBLISH",
    riskLevel = RiskLevel.WRITE,
    requiredPermission = "DOC_APPROVE",
    description = "Publish document to IBM i IFS"
)
@Component
@RequiredArgsConstructor
public class DocPublishExecutor implements OperationExecutor {

    private static final Pattern SAFE_DOC_ID = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern SAFE_IFS_PATH = Pattern.compile("^/QOpenSys/[A-Za-z0-9_./-]+$");

    private final AS400ClientProvider clientProvider;

    @Override
    public List<String> defineSteps() {
        return List.of("VALIDATE_DOC", "PUBLISH_TO_IFS", "UPDATE_DOC_PATH", "VERIFY_PUBLISH");
    }

    @Override
    public StepResult executeStep(Operation op, String stepCode) {
        AS400Client client = clientProvider.current();
        long start = System.currentTimeMillis();

        return switch (stepCode) {
            case "VALIDATE_DOC" -> {
                String docId = OperationJsonUtil.extractField(op, "docId");
                String content = OperationJsonUtil.extractField(op, "content");
                String fileType = OperationJsonUtil.extractField(op, "fileType");

                if (docId == null || docId.isBlank()) {
                    yield StepResult.failed("INVALID_PARAM", "Missing parameter", "docId is required",
                        System.currentTimeMillis() - start);
                }
                if (!SAFE_DOC_ID.matcher(docId).matches()) {
                    yield StepResult.failed("INVALID_PARAM", "Invalid docId", "docId must contain only alphanumeric, dash, or underscore",
                        System.currentTimeMillis() - start);
                }
                if ("PDF".equals(fileType) || "IMAGE".equals(fileType)) {
                    if (content == null || content.isBlank()) {
                        yield StepResult.failed("INVALID_PARAM", "Binary doc needs IFS path",
                            "PDF/IMAGE docs require uploaded file, not body content",
                            System.currentTimeMillis() - start);
                    }
                }
                yield StepResult.success("OK", "Document validated", System.currentTimeMillis() - start);
            }
            case "PUBLISH_TO_IFS" -> {
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
                    ? StepResult.success("OK", "Published to IFS: " + path, System.currentTimeMillis() - start)
                    : StepResult.failed("PUBLISH_FAILED", "Failed to write to IFS", path,
                        System.currentTimeMillis() - start);
            }
            case "UPDATE_DOC_PATH" -> {
                yield StepResult.success("OK", "Doc path update delegated to service layer",
                    System.currentTimeMillis() - start);
            }
            case "VERIFY_PUBLISH" -> {
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
                    yield StepResult.success("OK", "Publish verified", System.currentTimeMillis() - start);
                } else {
                    yield StepResult.failed("VERIFY_FAILED", "File not found after publish", path,
                        System.currentTimeMillis() - start);
                }
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown step: " + stepCode);
        };
    }
}