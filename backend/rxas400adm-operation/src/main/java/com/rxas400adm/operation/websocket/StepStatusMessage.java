package com.rxas400adm.operation.websocket;

/**
 * WebSocket 推送 Step 状态变更的消息体。
 * 前端订阅 /topic/operations/{operationId} 接收此消息。
 *
 * @param stepCode      步骤编码（如 SET_GROUP）
 * @param status        步骤状态（RUNNING / SUCCESS / FAILED / SKIPPED）
 * @param durationMs    执行耗时（毫秒），null 表示尚未完成
 * @param operationId   所属 Operation ID
 * @param operationStatus Operation 整体状态
 */
public record StepStatusMessage(
    String stepCode,
    String status,
    Long durationMs,
    Long operationId,
    String operationStatus
) {
}