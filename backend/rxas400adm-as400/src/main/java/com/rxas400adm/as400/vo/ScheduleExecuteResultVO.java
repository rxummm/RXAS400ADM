package com.rxas400adm.as400.vo;

/**
 * 调度任务执行结果（ScheduleController.executeNow 返回）。
 */
public record ScheduleExecuteResultVO(String status, String message, long costMs) {
}
