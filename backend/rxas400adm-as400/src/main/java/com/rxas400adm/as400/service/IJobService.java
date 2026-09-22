package com.rxas400adm.as400.service;

import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.SpoolRow;
import com.rxas400adm.as400.vo.JobInfo;
import com.rxas400adm.common.response.PageResult;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Job 中心：基于 QSYS2.ACTIVE_JOB_INFO() 的活动作业查询与作业控制。
 * 数据源按当前请求的 X-AS400-Server 头路由。
 */
public interface IJobService {

    List<JobInfo> activeJobs(String status);

    PageResult<JobInfo> activeJobsPage(String status, int current, int size);

    List<JobInfo> msgwJobs();

    List<JobInfo> lckwJobs();

    JobInfo jobDetail(String jobName, String jobUser, String jobNumber);

    CommandResult endJob(String jobName, String jobUser, String jobNumber);

    CommandResult holdJob(String jobName, String jobUser, String jobNumber);

    CommandResult releaseJob(String jobName, String jobUser, String jobNumber);

    List<Map<String, Object>> jobLog(String jobName, String jobUser, String jobNumber);

    List<Map<String, Object>> msgwMessages();

    PageResult<Map<String, Object>> msgwMessagesPage(int current, int size);

    List<JobQueueRow> jobQueues();

    List<SpoolRow> spoolFiles(String jobName, String jobUser, String jobNumber);

    PageResult<SpoolRow> spoolFilesPage(String jobName, String jobUser, String jobNumber, int current, int size);

    /** SPOOL 文件内容读取（文本流） */
    InputStream spoolFileContent(String jobName, String jobUser, String jobNumber,
                                 String spoolName, String outputQueue);

    /** 删除 SPOOL 文件（DLTSPLF） */
    CommandResult deleteSpoolFile(String jobName, String jobUser, String jobNumber,
                                  String spoolName, String outputQueue);

    CommandResult replyMsg(String jobName, String jobUser, String jobNumber);

    /** 历史日志查询（QSYS2.HISTORY_LOG_INFO） */
    List<Map<String, Object>> historyLog(String jobName, String fromDate, String toDate);

    /** 分页历史日志查询 */
    PageResult<Map<String, Object>> historyLogPage(String jobName, String fromDate, String toDate,
                                                   int current, int size);
}
