package com.rxas400adm.report;

import com.rxas400adm.report.dto.ReportScheduleDTO;
import com.rxas400adm.report.vo.ScheduleExecuteResultVO;

import java.util.List;

public interface IReportScheduleService {

    List<ReportSchedule> list();

    ReportSchedule create(ReportScheduleDTO schedule, String username);

    ReportSchedule update(Long id, ReportScheduleDTO schedule);

    void delete(Long id);

    ReportSchedule toggle(Long id, Boolean enabled);

    ScheduleExecuteResultVO executeNow(Long id);

    ScheduleExecuteResultVO execute(Long id);

    List<ReportScheduleHistoryVO> history(Long scheduleId);
}
