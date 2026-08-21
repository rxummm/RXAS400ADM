package com.rxas400adm.report.vo;

/**
 * 定时任务执行结果（ReportScheduleController.executeNow / ScheduleController.executeNow 返回）。
 */
public record ScheduleExecuteResultVO(String status, String message, long fileBytes) {
}
