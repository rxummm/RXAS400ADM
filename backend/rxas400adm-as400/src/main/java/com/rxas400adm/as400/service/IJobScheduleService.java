package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.JobScheduleRequest;
import com.rxas400adm.as400.entity.JobSchedule;
import com.rxas400adm.as400.entity.JobScheduleHistory;

import java.util.List;
import java.util.Map;

/**
 * 作业调度中心：定时执行 CL 命令 / SQL，任务持久化在 rx_job_schedule，
 * Quartz 按 cron 触发。启动时自动把启用中的任务重新注册到调度器。
 */
public interface IJobScheduleService {

    List<JobSchedule> list();

    JobSchedule create(JobScheduleRequest request, String username);

    JobSchedule update(Long id, JobScheduleRequest request);

    void delete(Long id);

    JobSchedule toggle(Long id, Boolean enabled);

    com.rxas400adm.as400.vo.ScheduleExecuteResultVO executeNow(Long id);

    com.rxas400adm.as400.vo.ScheduleExecuteResultVO execute(Long id);

    List<JobScheduleHistory> history(Long scheduleId);
}
