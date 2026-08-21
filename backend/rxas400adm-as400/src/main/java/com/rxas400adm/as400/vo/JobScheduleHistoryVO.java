package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.JobScheduleHistory;

import java.time.LocalDateTime;

/**
 * 作业调度执行历史视图（P2-10）：与 JobScheduleHistory 字段一致。
 */
public record JobScheduleHistoryVO(
        Long id,
        Long scheduleId,
        LocalDateTime runTime,
        String status,
        String message,
        Long costMs) {

    public static JobScheduleHistoryVO from(JobScheduleHistory e) {
        return new JobScheduleHistoryVO(
                e.getId(), e.getScheduleId(), e.getRunTime(), e.getStatus(), e.getMessage(), e.getCostMs());
    }
}
