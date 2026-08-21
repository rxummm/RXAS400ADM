package com.rxas400adm.config.vo;

import java.time.LocalDateTime;

/**
 * 任务手动触发结果（PlatformTaskController.trigger 返回）。
 */
public record TaskTriggerVO(boolean triggered, LocalDateTime time) {
}
