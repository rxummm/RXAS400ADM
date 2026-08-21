package com.rxas400adm.system.vo;

/**
 * 批量删除结果（NotificationController.batchDelete / WebhookController.cleanLogs 返回）。
 */
public record BatchDeleteResultVO(int deleted) {
}
