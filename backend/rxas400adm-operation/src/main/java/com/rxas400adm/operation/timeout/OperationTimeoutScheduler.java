package com.rxas400adm.operation.timeout;

import com.rxas400adm.operation.config.OperationProperties;
import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.mapper.OperationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationTimeoutScheduler {

    private final OperationMapper operationMapper;
    private final OperationProperties properties;

    private static final int SCAN_LIMIT = 100;

    /** 仅作最终回退：配置缺失或配置值无效时使用静态默认，优先读取自 `OperationProperties.timeouts`。 */
    private static final Map<String, Duration> DEFAULT_TIMEOUTS = Map.of(
        "USER_CREATE", Duration.ofMinutes(5),
        "USER_UPDATE", Duration.ofMinutes(3),
        "USER_DELETE", Duration.ofMinutes(3),
        "END_JOB", Duration.ofMinutes(2),
        "END_SUBSYSTEM", Duration.ofMinutes(3),
        "IFS_WRITE", Duration.ofMinutes(10),
        "IFS_DELETE", Duration.ofMinutes(5),
        "RAW_CL", Duration.ofMinutes(1),
        "SPOOL_DELETE", Duration.ofMinutes(3)
    );

    @Scheduled(fixedDelayString = "${rxas400.operation.cleanup-interval-ms:60000}")
    public void cleanupStuckOperations() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(properties.getStuckThresholdMinutes());
        List<Operation> stuckOps = operationMapper.findStuckOperations(threshold, SCAN_LIMIT);

        for (Operation op : stuckOps) {
            Duration timeout = getTimeout(op.getOperationType());
            if (op.getStartedAt() != null && Duration.between(op.getStartedAt(), LocalDateTime.now()).compareTo(timeout) > 0) {
                log.warn("Operation timed out: id={}, type={}, started={}, timeout={}",
                    op.getId(), op.getOperationType(), op.getStartedAt(), timeout);
                operationMapper.markFailed(op.getId(), "OPERATION_TIMEOUT",
                    "Operation timed out (threshold " + timeout.toMinutes() + " minutes)", op.getVersion());
            }
        }
    }

    private Duration getTimeout(String operationType) {
        if (operationType == null) {
            return DEFAULT_TIMEOUTS.getOrDefault("RAW_CL", Duration.ofMinutes(1));
        }
        Map<String, Long> timeouts = properties.getTimeouts();
        if (timeouts != null && timeouts.containsKey(operationType)) {
            long millis = timeouts.get(operationType);
            if (millis > 0) {
                return Duration.ofMillis(millis);
            }
        }
        return DEFAULT_TIMEOUTS.getOrDefault(operationType, Duration.ofMinutes(5));
    }
}