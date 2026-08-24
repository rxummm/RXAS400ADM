package com.rxas400adm.as400;

import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.GraphLink;
import com.rxas400adm.as400.model.GraphNode;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.JobSlaExecRow;
import com.rxas400adm.as400.model.SpoolRow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
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
        return sqlClient.queryList("SELECT JOB_QUEUE_NAME, JOB_QUEUE_LIBRARY, JOB_QUEUE_STATUS, "
                + "NUMBER_OF_JOBS, JOB_QUEUE_TYPE FROM QSYS2.JOB_QUEUE_INFO").stream()
                .map(r -> new JobQueueRow(str(r, "JOB_QUEUE_NAME"), str(r, "JOB_QUEUE_LIBRARY"),
                        str(r, "JOB_QUEUE_STATUS"), lng(r, "NUMBER_OF_JOBS"), str(r, "JOB_QUEUE_TYPE")))
                .toList();
    }

    @Override
    public List<SpoolRow> listSpoolFiles(String jobName, String jobUser, String jobNumber) {
        StringBuilder sql = new StringBuilder(
                "SELECT SPOOLED_FILE_NAME, JOB_NAME, JOB_USER, JOB_NUMBER, OUTPUT_QUEUE, "
                        + "OUTPUT_QUEUE_LIBRARY, SPOOLED_FILE_STATUS, NUMBER_OF_PAGES, USER_DATA "
                        + "FROM QSYS2.OUTPUT_QUEUE_INFO WHERE 1=1");
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
    public List<JobSlaExecRow> jobSlaExecutions() {
        String sql = "SELECT JOB_NUMBER, JOB_NAME, JOB_USER, "
                + "MIN(MESSAGE_TIMESTAMP) AS START_TS, MAX(MESSAGE_TIMESTAMP) AS END_TS "
                + "FROM TABLE(QSYS2.JOB_LOG_INFO(JOB_NAME_FILTER => '*ALL', JOB_USER_FILTER => '*ALL', "
                + "JOB_NUMBER_FILTER => '*ALL')) X "
                + "GROUP BY JOB_NUMBER, JOB_NAME, JOB_USER ORDER BY END_TS DESC FETCH FIRST 50 ROWS ONLY";
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
        String sql = "SELECT JOB_NUMBER, JOB_NAME, JOB_USER, MESSAGE_TEXT "
                + "FROM TABLE(QSYS2.JOB_LOG_INFO(JOB_NAME_FILTER => '*ALL', JOB_USER_FILTER => '*ALL', "
                + "JOB_NUMBER_FILTER => '*ALL', MESSAGE_ID_FILTER => 'CPF1124')) X "
                + "FETCH FIRST 200 ROWS ONLY";
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
            java.sql.Timestamp f = toTimestamp(from);
            java.sql.Timestamp t = toTimestamp(to);
            return Duration.between(f.toInstant(), t.toInstant()).getSeconds();
        } catch (Exception e) {
            log.debug("作业耗时解析失败(host={}): {}", state.host, state.redact(e.getMessage()));
            return -1;
        }
    }

    private java.sql.Timestamp toTimestamp(Object o) {
        if (o instanceof java.sql.Timestamp ts) {
            return ts;
        }
        if (o instanceof java.time.LocalDateTime ldt) {
            return java.sql.Timestamp.valueOf(ldt);
        }
        return java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(
                String.valueOf(o).trim().replace(' ', 'T')));
    }


}