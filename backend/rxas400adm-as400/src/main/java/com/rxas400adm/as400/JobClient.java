package com.rxas400adm.as400;

import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.JobSlaExecRow;
import com.rxas400adm.as400.model.SpoolRow;

import java.util.List;

/**
 * 作业域：作业队列、SPOOL 文件、作业 SLA 执行、作业依赖图。
 */
public interface JobClient {

    /**
     * 作业队列列表（2.1.8）：QSYS2.JOB_QUEUE_INFO。
     */
    List<JobQueueRow> listJobQueues();

    /**
     * SPOOL 文件列表（2.1.8）：可按作业过滤。
     */
    List<SpoolRow> listSpoolFiles(String jobName, String jobUser, String jobNumber);

    /**
     * 作业 SLA 最近执行情况。mock 模式返回仿真数据；JT400 需真机（依赖作业历史），当前返回空列表。
     */
    List<JobSlaExecRow> jobSlaExecutions();

    /**
     * 作业依赖图。mock 模式返回仿真图；JT400 需真机（依赖作业调度/日志），当前返回空图。
     */
    GraphData jobDependencies();
}
