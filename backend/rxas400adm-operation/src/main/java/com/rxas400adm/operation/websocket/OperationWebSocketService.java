package com.rxas400adm.operation.websocket;

import com.rxas400adm.operation.domain.Operation;
import com.rxas400adm.operation.domain.OperationStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Operation Step 状态变更实时推送。
 * 遵循架构文档 §19.8 定义的 WebSocket 协议：
 * - 订阅地址：/topic/operations/{id}
 * - 消息体：{ "stepCode": "SET_GROUP", "status": "SUCCESS", "durationMs": 850, "operationId": 100001, "operationStatus": "RUNNING" }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送 Step 状态变更到订阅了该 Operation 的所有前端。
     */
    public void publishStepStatus(Operation operation, OperationStep step) {
        StepStatusMessage message = new StepStatusMessage(
            step.getStepCode(),
            step.getStatus(),
            step.getDurationMs(),
            operation.getId(),
            operation.getStatus()
        );

        String destination = "/topic/operations/" + operation.getId();
        log.debug("Publishing step status to {}: {}", destination, message);
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * 推送 Operation 整体状态变更（如完成/失败/取消）。
     */
    public void publishOperationStatus(Operation operation) {
        StepStatusMessage message = new StepStatusMessage(
            operation.getCurrentStep(),
            operation.getStatus(),
            null,
            operation.getId(),
            operation.getStatus()
        );

        String destination = "/topic/operations/" + operation.getId();
        log.debug("Publishing operation status to {}: {}", destination, message);
        messagingTemplate.convertAndSend(destination, message);
    }
}