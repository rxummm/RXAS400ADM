package com.rxas400adm.as400;

import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.GraphLink;
import com.rxas400adm.as400.model.GraphNode;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.JobSlaExecRow;
import com.rxas400adm.as400.model.SpoolRow;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.IFSFileInputStream;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.rxas400adm.as400.JTOpenConnectionState.str;
import static com.rxas400adm.as400.JTOpenConnectionState.lng;

/**
 * JTOpen JobClient 委托实现（作业队列 / SPOOL / SLA / 依赖图）。
 */
@Slf4j
class JTOpenJobClient implements JobClient {

    private static final Set<String> SYSTEM_JOB_NAMES = Set.of(
            "QP0ZSPWP", "QP0ZSPWQ", "QZDASOINIT", "QSQSRVR", "QPJOBLOG",
            "QJRN", "QHTTPSVR", "QSYSARB", "QPWFSERVSD");

    private final JTOpenConnectionState state;
    private final JTOpenSqlClient sqlClient;

    JTOpenJobClient(JTOpenConnectionState state) {
        this.state = state;
        this.sqlClient = new JTOpenSqlClient(state);
    }

    @Override
    public List<JobQueueRow> listJobQueues() {
        return sqlClient.queryList(SqlStatementRegistry.of("job.queue.list")).stream()
                .map(r -> new JobQueueRow(str(r, "JOB_QUEUE_NAME"), str(r, "JOB_QUEUE_LIBRARY"),
                        str(r, "JOB_QUEUE_STATUS"), lng(r, "NUMBER_OF_JOBS"), str(r, "JOB_QUEUE_TYPE")))
                .toList();
    }

    @Override
    public List<SpoolRow> listSpoolFiles(String jobName, String jobUser, String jobNumber) {
        StringBuilder sql = new StringBuilder(SqlStatementRegistry.of("job.spool.list.base"));
        List<Object> params = new ArrayList<>();
        if (jobName != null && !jobName.isBlank()) {
            sql.append(" AND JOB_NAME = ?");
            params.add(jobName.trim().toUpperCase());
        }
        if (jobUser != null && !jobUser.isBlank()) {
            sql.append(" AND JOB_USER = ?");
            params.add(jobUser.trim().toUpperCase());
        }
        if (jobNumber != null && !jobNumber.isBlank()) {
            sql.append(" AND JOB_NUMBER = ?");
            params.add(jobNumber.trim());
        }
        sql.append(" FETCH FIRST 200 ROWS ONLY");
        return sqlClient.queryList(sql.toString(), params.toArray()).stream().map(r -> new SpoolRow(
                str(r, "SPOOLED_FILE_NAME"), str(r, "JOB_NAME"), str(r, "JOB_USER"),
                str(r, "JOB_NUMBER"), str(r, "OUTPUT_QUEUE"), str(r, "SPOOLED_FILE_STATUS"),
                lng(r, "NUMBER_OF_PAGES"), str(r, "USER_DATA"))).toList();
    }

    @Override
    public InputStream spoolFileContent(String jobName, String jobUser, String jobNumber,
                                        String spoolName, String outputQueue) {
        String tempPath = null;
        try {
            AS400 system = state.connect();
            tempPath = "/tmp/rxas400/spool/" + java.util.UUID.randomUUID() + ".txt";
            String cmd = String.format(
                    "DSPSPLF FILE(%s) JOB(%s/%s/%s) SPLNBR(*SELECT) OUTPUT(%s) OUTTYPE(*OUTFILE) OUTFILE(QTEMP/SPLFOUT)",
                    spoolName.trim().toUpperCase(),
                    jobNumber.trim(), jobUser.trim().toUpperCase(),
                    jobName.trim().toUpperCase(), tempPath);
            CommandCall call = new CommandCall(system);
            boolean ok = call.run(cmd);
            if (!ok) {
                StringBuilder sb = new StringBuilder();
                for (AS400Message msg : call.getMessageList()) {
                    sb.append(msg.getText());
                }
                log.warn("DSPSPLF 失败(host={}, spool={}): {}", state.host, spoolName, sb);
                return null;
            }
            IFSFileInputStream in = new IFSFileInputStream(system, tempPath);
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
                return new ByteArrayInputStream(baos.toByteArray());
            } finally {
                in.close();
            }
        } catch (Exception e) {
            log.warn("读取 SPOOL 文件内容失败(host={}, spool={}): {}",
                    state.host, spoolName, state.redact(e.getMessage()));
            return null;
        } finally {
            if (tempPath != null) {
                try {
                    AS400 system = state.connect();
                    new CommandCall(system).run("DLTF FILE(" + tempPath + ")");
                } catch (Exception ignored) {
                    log.debug("SPOOL 临时文件清理失败: {}", tempPath);
                }
            }
        }
    }

    @Override
    public CommandResult deleteSpoolFile(String jobName, String jobUser, String jobNumber,
                                         String spoolName, String outputQueue) {
        try {
            AS400 system = state.connect();
            String queue = outputQueue != null && !outputQueue.isBlank()
                    ? outputQueue.trim().toUpperCase() : "*SELECT";
            String cmd = String.format(
                    "DLTSPLF FILE(%s) JOB(%s/%s/%s) SPLNBR(*SELECT) OUTPUT(%s)",
                    spoolName.trim().toUpperCase(),
                    jobNumber.trim(), jobUser.trim().toUpperCase(),
                    jobName.trim().toUpperCase(), queue);
            CommandCall call = new CommandCall(system);
            boolean ok = call.run(cmd);
            StringBuilder sb = new StringBuilder();
            for (AS400Message msg : call.getMessageList()) {
                sb.append(msg.getText()).append('\n');
            }
            return ok ? CommandResult.ok(sb.toString().isBlank() ? "SPOOL 文件已删除" : sb.toString())
                    : CommandResult.fail(sb.toString().isBlank() ? "SPOOL 文件删除失败" : sb.toString());
        } catch (Exception e) {
            log.warn("删除 SPOOL 文件失败(host={}, spool={}): {}",
                    state.host, spoolName, state.redact(e.getMessage()));
            return CommandResult.fail("删除失败: " + state.redact(e.getMessage()));
        }
    }

    @Override
    public List<JobSlaExecRow> jobSlaExecutions() {
        String sql = SqlStatementRegistry.of("job.sla.executions");
        List<Map<String, Object>> rows = sqlClient.queryList(sql);
        List<JobSlaExecRow> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String job = jobShortName(String.valueOf(row.getOrDefault("JOB_NAME", "")));
            if (job.isBlank() || SYSTEM_JOB_NAMES.contains(job) || !seen.add(job)) {
                continue;
            }
            long elapsed = tsDiff(row.get("START_TS"), row.get("END_TS"));
            if (elapsed <= 0) {
                continue;
            }
            result.add(new JobSlaExecRow(job, "", 0, elapsed, ""));
        }
        return result;
    }

    @Override
    public GraphData jobDependencies() {
        String sql = SqlStatementRegistry.of("job.dependencies");
        List<Map<String, Object>> rows = sqlClient.queryList(sql);
        Map<String, String> nodes = new LinkedHashMap<>();
        List<GraphLink> links = new ArrayList<>();
        Set<String> seenLinks = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String child = jobShortName(String.valueOf(row.getOrDefault("JOB_NAME", "")));
            String parent = parentFromCpf1124(String.valueOf(row.getOrDefault("MESSAGE_TEXT", "")));
            if (child.isBlank() || parent.isBlank() || child.equalsIgnoreCase(parent)) {
                continue;
            }
            nodes.putIfAbsent(child, "PGM");
            nodes.putIfAbsent(parent, "PGM");
            if (seenLinks.add(parent + "->" + child)) {
                links.add(new GraphLink(parent, child));
            }
        }
        List<GraphNode> nodeRows = new ArrayList<>();
        for (Map.Entry<String, String> e : nodes.entrySet()) {
            nodeRows.add(new GraphNode(e.getKey(), e.getKey(), e.getValue(), null));
        }
        return new GraphData(nodeRows, links);
    }

    private String jobShortName(String jobName) {
        if (jobName == null) {
            return "";
        }
        String[] parts = jobName.split("/");
        return parts.length == 3 ? parts[2].trim() : jobName.trim();
    }

    private String parentFromCpf1124(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int idx = text.toLowerCase().indexOf("submitted by job");
        if (idx < 0) {
            return "";
        }
        return jobShortName(text.substring(idx + "submitted by job".length()).trim());
    }

    private long tsDiff(Object from, Object to) {
        if (from == null || to == null) {
            return -1;
        }
        try {
            Timestamp f = toTimestamp(from);
            Timestamp t = toTimestamp(to);
            return Duration.between(f.toInstant(), t.toInstant()).getSeconds();
        } catch (Exception e) {
            log.debug("作业耗时解析失败(host={}): {}", state.host, state.redact(e.getMessage()));
            return -1;
        }
    }

    private Timestamp toTimestamp(Object o) {
        if (o instanceof Timestamp ts) {
            return ts;
        }
        if (o instanceof LocalDateTime ldt) {
            return Timestamp.valueOf(ldt);
        }
        return Timestamp.valueOf(LocalDateTime.parse(
                String.valueOf(o).trim().replace(' ', 'T')));
    }

    @Override
    public List<Map<String, Object>> historyLog(String jobName, String fromDate, String toDate) {
        StringBuilder sql = new StringBuilder(SqlStatementRegistry.of("job.history.log"));
        List<Object> params = new ArrayList<>();
        if (jobName != null && !jobName.isBlank()) {
            sql.append(" AND UPPER(JOB_NAME) LIKE ?");
            params.add("%" + jobName.trim().toUpperCase() + "%");
        }
        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND MESSAGE_TIMESTAMP >= ?");
            params.add(fromDate.trim());
        }
        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND MESSAGE_TIMESTAMP <= ?");
            params.add(toDate.trim());
        }
        sql.append(" ORDER BY MESSAGE_TIMESTAMP DESC FETCH FIRST 500 ROWS ONLY");
        return sqlClient.queryList(sql.toString(), params.toArray());
    }

}