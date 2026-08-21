package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.JobSchedule;

import java.time.LocalDateTime;

/**
 * 作业调度任务视图（P2-10）：与 JobSchedule 字段一致、与前端 JSON 契约解耦，
 * 防止表结构新增内部字段时自动泄漏到 API。
 */
public record JobScheduleVO(
        Long id,
        String name,
        String description,
        Long serverId,
        String scheduleType,
        String command,
        String cronExpr,
        Boolean enabled,
        String status,
        LocalDateTime lastRunTime,
        String lastResult,
        String createdBy,
        LocalDateTime createdTime,
        LocalDateTime updatedTime) {

    public static JobScheduleVO from(JobSchedule e) {
        return new JobScheduleVO(
                e.getId(), e.getName(), e.getDescription(), e.getServerId(), e.getScheduleType(),
                e.getCommand(), e.getCronExpr(), e.getEnabled(), e.getStatus(), e.getLastRunTime(),
                e.getLastResult(), e.getCreatedBy(), e.getCreatedTime(), e.getUpdatedTime());
    }
}
