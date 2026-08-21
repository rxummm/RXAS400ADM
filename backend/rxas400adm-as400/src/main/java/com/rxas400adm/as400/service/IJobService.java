package com.rxas400adm.as400.service;

import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.SpoolRow;
import com.rxas400adm.as400.vo.JobInfo;

import java.util.List;
import java.util.Map;

/**
 * Job 中心：基于 QSYS2.ACTIVE_JOB_INFO() 的活动作业查询与作业控制。
 * 数据源按当前请求的 X-AS400-Server 头路由。
 */
public interface IJobService {

    List<JobInfo> activeJobs(String status);

    List<JobInfo> msgwJobs();

    List<JobInfo> lckwJobs();

    JobInfo jobDetail(String jobName, String jobUser, String jobNumber);

    CommandResult endJob(String jobName, String jobUser, String jobNumber);

    CommandResult holdJob(String jobName, String jobUser, String jobNumber);

    CommandResult releaseJob(String jobName, String jobUser, String jobNumber);

    List<Map<String, Object>> jobLog(String jobName, String jobUser, String jobNumber);

    List<Map<String, Object>> msgwMessages();

    List<JobQueueRow> jobQueues();

    List<SpoolRow> spoolFiles(String jobName, String jobUser, String jobNumber);

    CommandResult replyMsg(String jobName, String jobUser, String jobNumber);
}
