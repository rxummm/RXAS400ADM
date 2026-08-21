package com.rxas400adm.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportScheduleVO {

    private Long id;
    private String name;
    private String reportType;
    private String format;
    private Long serverId;
    private Integer days;
    private String cronExpr;
    private Boolean enabled;
    private String status;
    private LocalDateTime lastRunTime;
    private String lastResult;
    private LocalDateTime createdTime;

    public static ReportScheduleVO from(ReportSchedule entity) {
        ReportScheduleVO vo = new ReportScheduleVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setReportType(entity.getReportType());
        vo.setFormat(entity.getFormat());
        vo.setServerId(entity.getServerId());
        vo.setDays(entity.getDays());
        vo.setCronExpr(entity.getCronExpr());
        vo.setEnabled(entity.getEnabled());
        vo.setStatus(entity.getStatus());
        vo.setLastRunTime(entity.getLastRunTime());
        vo.setLastResult(entity.getLastResult());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}