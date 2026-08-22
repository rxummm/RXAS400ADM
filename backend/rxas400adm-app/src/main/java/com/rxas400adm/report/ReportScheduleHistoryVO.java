package com.rxas400adm.report;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表定时任务执行历史视图对象（B5：禁止直接返回 Entity）。
 */
@Data
public class ReportScheduleHistoryVO {

    private Long id;
    private Long scheduleId;
    private LocalDateTime runTime;
    private String status;
    private String message;
    private Long fileBytes;
    private LocalDateTime createdTime;

    public static ReportScheduleHistoryVO from(ReportScheduleHistory entity) {
        ReportScheduleHistoryVO vo = new ReportScheduleHistoryVO();
        vo.setId(entity.getId());
        vo.setScheduleId(entity.getScheduleId());
        vo.setRunTime(entity.getRunTime());
        vo.setStatus(entity.getStatus());
        vo.setMessage(entity.getMessage());
        vo.setFileBytes(entity.getFileBytes());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
