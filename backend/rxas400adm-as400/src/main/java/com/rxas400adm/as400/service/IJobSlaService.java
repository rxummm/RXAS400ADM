package com.rxas400adm.as400.service;

import com.rxas400adm.as400.dto.JobSlaDTO;
import com.rxas400adm.as400.entity.JobSla;

import java.util.List;
import java.util.Map;

/**
 * 作业 SLA：本地维护关键作业预期耗时规则，最近执行数据取自 AS400Client.jobSlaExecutions()。
 */
public interface IJobSlaService {

    List<JobSla> list();

    JobSla create(JobSlaDTO sla, String username);

    JobSla update(Long id, JobSlaDTO sla);

    void delete(Long id);

    List<Map<String, Object>> executions();
}
