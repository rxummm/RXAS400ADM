package com.rxas400adm.as400;

import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.JobSlaExecRow;
import com.rxas400adm.as400.model.SpoolRow;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 作业域：作业队列、SPOOL 文件（列表/内容/删除）、作业 SLA 执行、作业依赖图。
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
     * SPOOL 文件内容读取：返回文本内容流。
     * @param jobName   作业名
     * @param jobUser   作业用户
     * @param jobNumber 作业号
     * @param spoolName SPOOL 文件名
     * @param outputQueue 输出队列名（可为 null，取第一个匹配）
     * @return 文本内容流（调用方负责关闭）；找不到返回 null
     */
    InputStream spoolFileContent(String jobName, String jobUser, String jobNumber,
                                 String spoolName, String outputQueue);

    /**
     * 删除 SPOOL 文件（DLTSPLF）。
     * @return CommandResult 表示成功/失败
     */
    CommandResult deleteSpoolFile(String jobName, String jobUser, String jobNumber,
                                  String spoolName, String outputQueue);

    /**
     * 作业 SLA 最近执行情况。mock 模式返回仿真数据；JT400 需真机（依赖作业历史），当前返回空列表。
     */
    List<JobSlaExecRow> jobSlaExecutions();

    /**
     * 作业依赖图。mock 模式返回仿真图；JT400 需真机（依赖作业调度/日志），当前返回空图。
     */
    GraphData jobDependencies();

    /**
     * 历史日志查询（QSYS2.HISTORY_LOG_INFO）。
     * @param jobName 作业名（模糊，% 通配）
     * @param fromDate 起始日期 YYYYMMDD（可为 null）
     * @param toDate 结束日期 YYYYMMDD（可为 null）
     * @return 日志行列表
     */
    default List<Map<String, Object>> historyLog(String jobName, String fromDate, String toDate) {
        return List.of();
    }
}
