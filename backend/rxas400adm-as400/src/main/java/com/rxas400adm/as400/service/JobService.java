package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.SpoolRow;
import com.rxas400adm.as400.vo.JobInfo;
import com.rxas400adm.common.constants.As400Identifiers;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
/**
 * Job 中心：基于 QSYS2.ACTIVE_JOB_INFO() 的活动作业查询与作业控制。
 * 数据源按当前请求的 X-AS400-Server 头路由（AS400ClientProvider.current()）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService implements IJobService {

    private static final String ACTIVE_JOB_SQL = """
            SELECT JOB_NAME, JOB_USER, JOB_NUMBER, JOB_STATUS, JOB_PROGRAM, CPU_TIME, TEMPORARY_STORAGE
            FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) X
            """;

    /** 允许的作业状态过滤白名单：仅接受这些状态值，其余拒绝（防 SQL 注入） */
    private static final Set<String> ALLOWED_STATUS = Set.of(
            "MSGW", "LCKW", "RUN", "JOBQ", "JOBSCD", "COMM", "DEQW", "ALLOC", "SYS", "IOOW", "JCLS", "HLD");

    /** H4：作业标识符白名单（作业名/用户/编号），拼入 CL 命令前校验，防注入追加语句 */
    /** S6：收敛至共享常量（原四处独立复制正则） */
    private static final Pattern IDENTIFIER = As400Identifiers.IDENTIFIER;

    /** S6：作业号单独精确校验（1~6 位十进制），比通用标识符白名单更早拦截畸形输入 */
    private static final Pattern JOB_NUMBER = As400Identifiers.JOB_NUMBER;

    private final AS400ClientProvider clientProvider;
    private final ProfileResolver profileResolver;
    private final ExecutorService msgwExecutor;

    /** 活动作业列表，可按状态过滤（MSGW / LCKW / RUN / JOBQ ...） */
    public List<JobInfo> activeJobs(String status) {
        AS400Client client = clientProvider.current();
        List<Map<String, Object>> rows;
        if (StringUtils.hasText(status)) {
            String upper = status.trim().toUpperCase();
            if (!ALLOWED_STATUS.contains(upper)) {
                throw new BusinessException(ErrorCode.JOB_INVALID_STATUS, "Invalid job status: " + status);
            }
            rows = client.queryList(ACTIVE_JOB_SQL + " WHERE JOB_STATUS = ?", upper);
        } else {
            rows = client.queryList(ACTIVE_JOB_SQL);
        }
        return rows.stream().map(JobInfo::from).toList();
    }

    /** MSGW 作业（生产关键） */
    public List<JobInfo> msgwJobs() {
        return activeJobs("MSGW");
    }

    /** LCKW 作业（锁等待/死锁风险） */
    public List<JobInfo> lckwJobs() {
        return activeJobs("LCKW");
    }

    /** 作业详情：按 作业名/用户/编号 精确匹配 */
    public JobInfo jobDetail(String jobName, String jobUser, String jobNumber) {
        if (!StringUtils.hasText(jobName)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Job name is required");
        }
        AS400Client client = clientProvider.current();
        // P10：JOB_NAME 过滤下推 DB2（保守包裹方案——表函数仍全量执行、WHERE 减少向 JVM 传输的数据量；
        // 未用 JOB_NAME_FILTER 参数因无法确证目标系统 DB2 for i 版本均 ≥7.2）。UPPER 双侧保持
        // 原 equalsIgnoreCase 大小写不敏感语义；其余 user/number 条件维持内存过滤不变。
        String sql = ACTIVE_JOB_SQL + " WHERE UPPER(JOB_NAME) = ?";
        return client.queryList(sql, jobName.trim().toUpperCase()).stream()
                .map(JobInfo::from)
                .filter(j -> j.getJobName().equalsIgnoreCase(jobName))
                .filter(j -> !StringUtils.hasText(jobUser) || j.getJobUser().equalsIgnoreCase(jobUser))
                .filter(j -> !StringUtils.hasText(jobNumber) || j.getJobNumber().equals(jobNumber))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND, "Job not found: " + jobName + "/" + jobUser));
    }

    /** ENDJOB 立即结束作业（权限 JOB_END，审计记录） */
    public CommandResult endJob(String jobName, String jobUser, String jobNumber) {
        requireJob(jobName, jobUser, jobNumber);
        AS400Client client = clientProvider.current();
        return client.execute("ENDJOB JOB(" + jobKey(jobName, jobUser, jobNumber) + ") OPTION(*IMMED)");
    }

    /** HLDJOB 挂起作业（暂停执行，不结束） */
    public CommandResult holdJob(String jobName, String jobUser, String jobNumber) {
        requireJob(jobName, jobUser, jobNumber);
        AS400Client client = clientProvider.current();
        return client.execute("HLDJOB JOB(" + jobKey(jobName, jobUser, jobNumber) + ")");
    }

    /** RLSJOB 释放被挂起的作业 */
    public CommandResult releaseJob(String jobName, String jobUser, String jobNumber) {
        requireJob(jobName, jobUser, jobNumber);
        AS400Client client = clientProvider.current();
        return client.execute("RLSJOB JOB(" + jobKey(jobName, jobUser, jobNumber) + ")");
    }

    /**
     * 作业日志（DSPJOBLOG）：基于 QSYS2.JOBLOG_INFO 表函数返回日志行。
     * 返回 [{ ordinal, messageId, messageType, messageText, messageTimestamp }]
     */
    public List<Map<String, Object>> jobLog(String jobName, String jobUser, String jobNumber) {
        requireJob(jobName, jobUser, jobNumber);
        AS400Client client = clientProvider.current();
        // P1-6：表函数入参用占位符绑定，替代 sq() 拼接
        String sql = SqlStatementRegistry.of("job.log.info");
        return client.queryList(sql, jobUser + "/" + jobName);
    }

    /**
     * MSGW 作业的等待消息列表：先取 MSGW 作业，再逐个取消息（含应答状态）。
     * 无真实环境（mock/降级）时返回仿真消息。
     * P9：原实现逐作业串行取消息，改为有界并行（Spring Bean msgwExecutor 固定并发），
     * futures 按作业顺序创建、按序 join——返回顺序与原串行实现一致。
     */
    public List<Map<String, Object>> msgwMessages() {
        List<JobInfo> msgwJobs = msgwJobs();
        // P9：ThreadLocal 服务器上下文只能在请求线程读——先捕获 client 再下发并行任务
        AS400Client client = clientProvider.current();
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>(msgwJobs.size());
        for (JobInfo job : msgwJobs) {
            futures.add(CompletableFuture.supplyAsync(() -> fetchMsgwMessageRow(client, job), msgwExecutor));
        }
        return futures.stream().map(CompletableFuture::join).toList();
    }

    /** P9：单作业消息抓取任务体；单个作业异常降级为该作业仿真行，不中断整体 */
    private Map<String, Object> fetchMsgwMessageRow(AS400Client client, JobInfo job) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("JOB_NAME", job.getJobName());
        row.put("JOB_USER", job.getJobUser());
        row.put("JOB_NUMBER", job.getJobNumber());
        try {
            // P1-6：表函数入参用占位符绑定，替代 sq() 拼接
            List<Map<String, Object>> msgs = client.queryList(
                    SqlStatementRegistry.of("job.msgw.detail.msgw"),
                    job.getJobUser() + "/" + job.getJobName());
            if (!msgs.isEmpty()) {
                msgs.stream().findFirst().ifPresent(row::putAll);
                return row;
            }
        } catch (Exception e) {
            if (!profileResolver.isMockMode()) {
                throw new BusinessException(ErrorCode.AS400_CONNECTION_FAILED,
                        "MSGW 查询失败(job=" + job.getJobUser() + "/" + job.getJobName() + "): " + e.getMessage());
            }
            log.warn("MSGW 查询失败(job={}/{}): {}", job.getJobUser(), job.getJobName(), e.getMessage());
        }
        // MSGW 作业的等待消息兜底（仅 mock 模式返回仿真数据）
        row.put("MESSAGE_ID", "CPF0000");
        row.put("MESSAGE_TYPE", "INQUIRY");
        row.put("MESSAGE_TEXT", "作业 " + job.getJobName() + " 等待消息应答（仿真）");
        row.put("REPLY_STATUS", "MSGW");
        return row;
    }

    /** 作业队列列表（2.1.8，WRKJOBQ / QSYS2.JOB_QUEUE_INFO） */
    public List<JobQueueRow> jobQueues() {
        return clientProvider.current().listJobQueues();
    }

    /** SPOOL 文件列表（2.1.8，WRKSPLF / QSYS2.OUTPUT_QUEUE_INFO），可按作业过滤 */
    public List<SpoolRow> spoolFiles(String jobName, String jobUser, String jobNumber) {
        return clientProvider.current().listSpoolFiles(jobName, jobUser, jobNumber);
    }

    /** SPOOL 文件内容读取（文本流） */
    public java.io.InputStream spoolFileContent(String jobName, String jobUser, String jobNumber,
                                                String spoolName, String outputQueue) {
        return clientProvider.current().spoolFileContent(jobName, jobUser, jobNumber, spoolName, outputQueue);
    }

    /** 删除 SPOOL 文件（DLTSPLF） */
    public CommandResult deleteSpoolFile(String jobName, String jobUser, String jobNumber,
                                         String spoolName, String outputQueue) {
        return clientProvider.current().deleteSpoolFile(jobName, jobUser, jobNumber, spoolName, outputQueue);
    }

    /** 应答 MSGW 作业的等待消息（RPLMSG，发送默认应答以解除 MSGW 状态） */
    public CommandResult replyMsg(String jobName, String jobUser, String jobNumber) {
        requireJob(jobName, jobUser, jobNumber);
        AS400Client client = clientProvider.current();
        return client.execute("RPLMSG MSGQ(" + jobKey(jobName, jobUser, jobNumber) + ") MSGKEY(*NONE) REPLY('I')");
    }

    /** 历史日志查询（QSYS2.HISTORY_LOG_INFO） */
    public List<Map<String, Object>> historyLog(String jobName, String fromDate, String toDate) {
        return clientProvider.current().historyLog(jobName, fromDate, toDate);
    }

    private void requireJob(String jobName, String jobUser, String jobNumber) {
        if (!StringUtils.hasText(jobName) || !StringUtils.hasText(jobUser) || !StringUtils.hasText(jobNumber)) {
            throw new BusinessException(ErrorCode.NAME_REQUIRED, "Job name/user/number is required");
        }
        if (!IDENTIFIER.matcher(jobName.trim()).matches()
                || !IDENTIFIER.matcher(jobUser.trim()).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "作业名/用户只能是字母/数字/下划线/$/#/@ 等合法标识符");
        }
        // S6：作业号精确校验为 1~6 位十进制（IBM i 约定），比通用标识符白名单更早拦截畸形输入
        if (!JOB_NUMBER.matcher(jobNumber.trim()).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Job number must be 1-6 digits: " + jobNumber);
        }
    }

    private String jobKey(String jobName, String jobUser, String jobNumber) {
        return jobNumber.trim() + "/" + jobUser.trim() + "/" + jobName.trim();
    }
}
