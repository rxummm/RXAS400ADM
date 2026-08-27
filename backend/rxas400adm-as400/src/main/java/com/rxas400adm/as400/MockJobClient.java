package com.rxas400adm.as400;

import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.GraphLink;
import com.rxas400adm.as400.model.GraphNode;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.JobSlaExecRow;
import com.rxas400adm.as400.model.SpoolRow;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mock JobClient 委托实现（作业队列 / SPOOL / SLA / 依赖图）。
 */
class MockJobClient implements JobClient {

    MockJobClient(MockState state) {
    }

    @Override
    public List<JobQueueRow> listJobQueues() {
        return List.of(
                new JobQueueRow("QBATCH", "QUSRSYS", "ACTIVE", 3L, "BATCH"),
                new JobQueueRow("QINTER", "QUSRSYS", "ACTIVE", 12L, "INTERACTIVE"),
                new JobQueueRow("QCMDQ", "QSYS", "ACTIVE", 1L, "BATCH"),
                new JobQueueRow("QSYSPRT", "QUSRSYS", "RELEASED", 5L, "BATCH"));
    }

    @Override
    public List<SpoolRow> listSpoolFiles(String jobName, String jobUser, String jobNumber) {
        List<SpoolRow> rows = new ArrayList<>();
        String[][] spools = {
                {"QPRINT01", "BATCH01", "QSECOFR", "129301", "QPRINT", "READY", "42", "RPT"},
                {"QPRINT02", "BATCH02", "QSECOFR", "129302", "QPRINT", "HELD", "8", "STD"},
                {"QPRINT03", "PAYROLL", "QSECOFR", "129305", "QPRINT", "READY", "120", "PAY"}
        };
        for (String[] s : spools) {
            if (jobName != null && !jobName.isBlank() && !jobName.equalsIgnoreCase(s[1])) {
                continue;
            }
            if (jobUser != null && !jobUser.isBlank() && !jobUser.equalsIgnoreCase(s[2])) {
                continue;
            }
            if (jobNumber != null && !jobNumber.isBlank() && !jobNumber.equals(s[3])) {
                continue;
            }
            rows.add(new SpoolRow(s[0], s[1], s[2], s[3], s[4], s[5], Long.parseLong(s[6]), s[7]));
        }
        return rows;
    }

    @Override
    public InputStream spoolFileContent(String jobName, String jobUser, String jobNumber,
                                        String spoolName, String outputQueue) {
        // Mock: 返回仿真 SPOOL 文件内容
        String content = String.format(
                "===== SPOOL FILE: %s =====\n" +
                "Job: %s/%s/%s\n" +
                "Output Queue: %s\n" +
                "Status: READY\n" +
                "================================\n\n" +
                "  RXAS400ADM SYSTEM REPORT\n" +
                "  Generated: 2026-08-27\n" +
                "\n" +
                "  Line 1: System status is NORMAL\n" +
                "  Line 2: All subsystems ACTIVE\n" +
                "  Line 3: Disk usage 67%%\n" +
                "  Line 4: CPU utilization 23%%\n" +
                "\n" +
                "  End of report.",
                spoolName, jobName, jobUser, jobNumber, outputQueue);
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public CommandResult deleteSpoolFile(String jobName, String jobUser, String jobNumber,
                                         String spoolName, String outputQueue) {
        return CommandResult.ok("[Mock] SPOOL 文件已删除: " + spoolName);
    }

    @Override
    public List<JobSlaExecRow> jobSlaExecutions() {
        String[][] jobs = {
                {"ORDNIGHT", "夜间订单批处理", "900"},
                {"INVRECON", "库存对账", "600"},
                {"DAILYBKUP", "每日备份", "1800"},
                {"PAYROLL", "工资计算", "1200"}
        };
        List<JobSlaExecRow> rows = new ArrayList<>();
        for (String[] j : jobs) {
            int expected = Integer.parseInt(j[2]);
            int actual = expected + (MockState.RANDOM.nextBoolean() ? -1 : 1) * MockState.RANDOM.nextInt(300);
            rows.add(new JobSlaExecRow(j[0], j[1], expected, Math.max(30, actual),
                    actual <= expected ? "OK" : "BREACHED"));
        }
        return rows;
    }

    @Override
    public GraphData jobDependencies() {
        String[][] nodes = {
                {"ORDNIGHT", "PGM"}, {"INVRECON", "PGM"}, {"DAILYBKUP", "PGM"},
                {"PAYROLL", "PGM"}, {"EMPCTL", "PGM"},
                {"ORDFILE", "FILE"}, {"INVFILE", "FILE"}, {"EMPFILE", "FILE"}
        };
        String[][] links = {
                {"ORDNIGHT", "INVRECON"}, {"ORDNIGHT", "DAILYBKUP"},
                {"INVRECON", "PAYROLL"}, {"DAILYBKUP", "PAYROLL"},
                {"PAYROLL", "EMPCTL"}, {"INVRECON", "INVFILE"}, {"ORDNIGHT", "ORDFILE"}
        };
        List<GraphNode> nodeRows = new ArrayList<>();
        for (String[] n : nodes) {
            nodeRows.add(new GraphNode(n[0], n[0], n[1], null));
        }
        List<GraphLink> linkRows = new ArrayList<>();
        for (String[] l : links) {
            linkRows.add(new GraphLink(l[0], l[1]));
        }
        return new GraphData(nodeRows, linkRows);
    }

    @Override
    public List<Map<String, Object>> historyLog(String jobName, String fromDate, String toDate) {
        // Mock: 返回仿真历史日志
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(MockState.row(
                "JOB_NAME", "ORDNIGHT", "JOB_USER", "QSECOFR", "JOB_NUMBER", "129301",
                "MESSAGE_ID", "CPF1124", "MESSAGE_TEXT", "Job started",
                "MESSAGE_TIMESTAMP", "2026-08-27 06:00:00", "SEVERITY_NUMBER", 0L));
        rows.add(MockState.row(
                "JOB_NAME", "ORDNIGHT", "JOB_USER", "QSECOFR", "JOB_NUMBER", "129301",
                "MESSAGE_ID", "CPF1125", "MESSAGE_TEXT", "Job ended normally",
                "MESSAGE_TIMESTAMP", "2026-08-27 06:15:00", "SEVERITY_NUMBER", 0L));
        rows.add(MockState.row(
                "JOB_NAME", "DAILYBKUP", "JOB_USER", "QSECOFR", "JOB_NUMBER", "129302",
                "MESSAGE_ID", "CPF3791", "MESSAGE_TEXT", "Save completed",
                "MESSAGE_TIMESTAMP", "2026-08-27 02:00:00", "SEVERITY_NUMBER", 0L));
        return rows;
    }
}