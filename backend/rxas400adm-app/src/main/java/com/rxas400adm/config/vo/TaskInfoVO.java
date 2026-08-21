package com.rxas400adm.config.vo;

import java.util.List;

/**
 * 定时任务信息（PlatformTaskController.tasks 返回）。
 */
public record TaskInfoVO(String bean, String className, List<ScheduledMethodVO> methods) {

    /**
     * 单个 @Scheduled 方法的描述。
     */
    public record ScheduledMethodVO(String method, String schedule, boolean enabled) {
    }
}
