package com.rxas400adm.report;

import com.rxas400adm.report.dto.ReportScheduleDTO;

import java.util.List;
import java.util.Map;

public interface IReportScheduleService {

    List<ReportSchedule> list();

    ReportSchedule create(ReportScheduleDTO schedule, String username);

    ReportSchedule update(Long id, ReportScheduleDTO schedule);

    void delete(Long id);

    ReportSchedule toggle(Long id, Boolean enabled);

    com.rxas400adm.report.vo.ScheduleExecuteResultVO executeNow(Long id);

    com.rxas400adm.report.vo.ScheduleExecuteResultVO execute(Long id);

    List<ReportScheduleHistory> history(Long scheduleId);
}