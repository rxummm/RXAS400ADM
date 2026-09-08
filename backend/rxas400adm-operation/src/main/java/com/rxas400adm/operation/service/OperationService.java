package com.rxas400adm.operation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.operation.annotation.IbmiOperation;
import com.rxas400adm.operation.config.OperationProperties;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.OperationStatus;
import com.rxas400adm.operation.domain.OperationStep;
import com.rxas400adm.operation.domain.RiskLevel;
import com.rxas400adm.operation.domain.StepResult;
import com.rxas400adm.operation.dto.CreateOperationRequest;
import com.rxas400adm.operation.executor.OperationExecutor;
import com.rxas400adm.operation.mapper.OperationMapper;
import com.rxas400adm.operation.mapper.OperationStepMapper;
import com.rxas400adm.operation.vo.OperationVO;
import com.rxas400adm.operation.websocket.OperationWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationMapper operationMapper;
    private final OperationStepMapper stepMapper;
    private final OperationStateMachine stateMachine;
    private final OperationRegistry registry;
    private final OperationProperties properties;
    private final OperationWebSocketService webSocketService;

    public PageResult<OperationVO> list(int pageNum, int pageSize) {
        IPage<Operation> page = operationMapper.selectPageOrderByRequestedAt(
            new Page<>(pageNum, pageSize));
        List<OperationVO> voList = page.getRecords().stream()
            .map(op -> {
                List<String> steps = resolveSteps(op.getOperationType());
                return OperationVO.from(op, steps);
            })
            .toList();
        PageResult<OperationVO> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(voList);
        return result;
    }

    public Operation get(Long id) {
        return EntityUtil.require(id, "Operation", operationMapper::selectById);
    }

    public Operation create(CreateOperationRequest request) {
        if (request.idempotencyKey() != null) {
            Operation existing = operationMapper.selectByIdempotencyKey(request.idempotencyKey());
            if (existing != null) {
                log.info("Idempotency hit, returning existing Operation: id={}, type={}", existing.getId(), request.operationType());
                return existing;
            }
        }

        OperationExecutor executor = getExecutor(request.operationType());
        RiskLevel riskLevel = resolveRiskLevel(request.operationType());

        Operation op = new Operation();
        op.setOperationType(request.operationType());
        op.setStatus(OperationStatus.REQUESTED.name());
        op.setTargetType(request.targetType());
        op.setTargetName(request.targetName());
        op.setRequestData(request.requestData());
        op.setIdempotencyKey(request.idempotencyKey());
        op.setRiskLevel(riskLevel.name());
        op.setRequestedBy(request.requestedBy());
        op.setRequestedAt(LocalDateTime.now());
        op.setRetryCount(0);
        op.setMaxRetry(properties.getMaxRetry());
        op.setVersion(1);
        operationMapper.insert(op);

        List<String> steps = executor.defineSteps();
        for (int i = 0; i < steps.size(); i++) {
            OperationStep step = new OperationStep();
            step.setOperationId(op.getId());
            step.setStepCode(steps.get(i));
            step.setStepOrder(i + 1);
            step.setStatus("NOT_EXECUTED");
            step.setRetryCount(0);
            stepMapper.insert(step);
        }

        log.info("Operation created: id={}, type={}, target={}/{}", op.getId(), request.operationType(), request.targetType(), request.targetName());
        return op;
    }

    public Operation execute(Long operationId) {
        Operation op = EntityUtil.require(operationId, "Operation", operationMapper::selectById);

        if (!stateMachine.advance(op, OperationStatus.RUNNING)) {
            throw new BusinessException(ErrorCode.OPERATION_STATUS_CONFLICT, "Operation status conflict, please refresh and retry");
        }
        op.setStartedAt(LocalDateTime.now());
        operationMapper.updateStartedAt(op.getId(), op.getStartedAt());

        OperationExecutor executor = getExecutor(op.getOperationType());
        List<String> steps = executor.defineSteps();

        int startIndex = resolveStartIndex(op, steps);

        // 预加载所有步骤的版本号，用于 CAS 乐观锁更新
        List<OperationStep> existingSteps = stepMapper.selectListByOperationId(op.getId());
        Map<String, Integer> stepVersions = new HashMap<>();
        for (OperationStep s : existingSteps) {
            stepVersions.put(s.getStepCode(), s.getVersion() != null ? s.getVersion() : 1);
        }

        for (int i = startIndex; i < steps.size(); i++) {
            String stepCode = steps.get(i);
            Integer currentVersion = stepVersions.getOrDefault(stepCode, 1);
            op.setCurrentStep(stepCode);
            operationMapper.updateCurrentStep(op.getId(), stepCode);

            stepMapper.updateStatus(op.getId(), stepCode, "RUNNING", currentVersion);
            // WebSocket 推送：步骤开始执行
            OperationStep runningStep = buildStepSnapshot(op.getId(), stepCode, "RUNNING", currentVersion + 1);
            webSocketService.publishStepStatus(op, runningStep);

            long start = System.currentTimeMillis();
            StepResult result;
            try {
                result = executor.executeStep(op, stepCode);
                StepResult verify = executor.verifyStep(op, stepCode);
                if (verify != null && verify.status() == StepResult.StepStatus.FAILED) {
                    result = verify;
                }
            } catch (Exception e) {
                result = StepResult.failed("EXCEPTION", e.getMessage(), e.getMessage(),
                    System.currentTimeMillis() - start);
            }

            long duration = System.currentTimeMillis() - start;
            // updateStatus 已使版本号 +1，此处传递新版本号
            stepMapper.updateResult(op.getId(), stepCode, result.status().name(),
                result.ibmiReturnCode(), result.ibmiMessage(), result.errorDetail(),
                duration, LocalDateTime.now(), currentVersion + 1);

            if (result.status() == StepResult.StepStatus.SUCCESS) {
                log.info("Step succeeded: op={}, step={}, duration={}ms", op.getId(), stepCode, duration);
                // WebSocket 推送：步骤成功
                OperationStep successStep = buildStepSnapshot(op.getId(), stepCode, "SUCCESS", currentVersion + 1);
                successStep.setDurationMs(duration);
                webSocketService.publishStepStatus(op, successStep);
            } else {
                log.warn("Step failed: op={}, step={}, error={}", op.getId(), stepCode, result.errorDetail());
                // WebSocket 推送：步骤失败
                OperationStep failedStep = buildStepSnapshot(op.getId(), stepCode, "FAILED", currentVersion + 1);
                failedStep.setDurationMs(duration);
                failedStep.setErrorDetail(result.errorDetail());
                webSocketService.publishStepStatus(op, failedStep);

                stateMachine.markFailed(op, "STEP_FAILED", "Step [" + stepCode + "] failed: " + result.errorDetail());
                operationMapper.updateCompletedAt(op.getId(), LocalDateTime.now());

                if (stateMachine.markRetrying(op)) {
                    log.info("Operation entering retry: op={}, retry={}", op.getId(), op.getRetryCount());
                    // WebSocket 推送：操作进入重试
                    webSocketService.publishOperationStatus(op);
                }
                return op;
            }
        }

        if (!stateMachine.advance(op, OperationStatus.SUCCESS)) {
            log.error("CAS conflict advancing to SUCCESS: op={}, version={}", op.getId(), op.getVersion());
            throw new BusinessException(ErrorCode.OPERATION_STATUS_CONFLICT,
                "Operation status conflict during completion, please refresh and retry");
        }
        op.setCompletedAt(LocalDateTime.now());
        operationMapper.updateCompletedAt(op.getId(), op.getCompletedAt());
        log.info("Operation completed: id={}, type={}, duration={}ms", op.getId(), op.getOperationType(),
            Duration.between(op.getStartedAt(), op.getCompletedAt()).toMillis());
        // WebSocket 推送：操作完成
        webSocketService.publishOperationStatus(op);

        return op;
    }

    public Operation retry(Long operationId) {
        Operation op = EntityUtil.require(operationId, "Operation", operationMapper::selectById);
        if (!OperationStatus.FAILED.name().equals(op.getStatus())
            && !OperationStatus.RETRYING.name().equals(op.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only FAILED/RETRYING operations can be retried");
        }
        return execute(operationId);
    }

    public void cancel(Long operationId) {
        Operation op = EntityUtil.require(operationId, "Operation", operationMapper::selectById);
        stateMachine.advance(op, OperationStatus.CANCELLED);
        operationMapper.updateCompletedAt(op.getId(), LocalDateTime.now());
        // WebSocket 推送：操作取消
        webSocketService.publishOperationStatus(op);
    }

    private OperationExecutor getExecutor(String operationType) {
        OperationExecutor executor = registry.get(operationType);
        if (executor == null) {
            throw new BusinessException(ErrorCode.OPERATION_TYPE_UNKNOWN, "Unknown operation type: " + operationType);
        }
        return executor;
    }

    private int resolveStartIndex(Operation op, List<String> steps) {
        if (op.getCurrentStep() == null) return 0;
        int idx = steps.indexOf(op.getCurrentStep());
        return idx >= 0 ? idx : 0;
    }

    private List<String> resolveSteps(String operationType) {
        var executor = registry.get(operationType);
        return executor != null ? executor.defineSteps() : Collections.emptyList();
    }

    private RiskLevel resolveRiskLevel(String operationType) {
        OperationExecutor executor = registry.get(operationType);
        if (executor == null) return RiskLevel.READ;
        IbmiOperation annotation = executor.getClass().getAnnotation(IbmiOperation.class);
        return annotation != null ? annotation.riskLevel() : RiskLevel.READ;
    }

    /**
     * 构建步进快照对象，用于 WebSocket 推送。
     * 不从数据库重新查询，避免不必要的 I/O。
     */
    private OperationStep buildStepSnapshot(Long operationId, String stepCode, String status, Integer version) {
        OperationStep step = new OperationStep();
        step.setOperationId(operationId);
        step.setStepCode(stepCode);
        step.setStatus(status);
        step.setVersion(version);
        return step;
    }
}