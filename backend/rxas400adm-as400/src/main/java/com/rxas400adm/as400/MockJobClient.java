package com.rxas400adm.as400;

import com.rxas400adm.as400.model.GraphData;
import com.rxas400adm.as400.model.GraphLink;
import com.rxas400adm.as400.model.GraphNode;
import com.rxas400adm.as400.model.JobQueueRow;
import com.rxas400adm.as400.model.JobSlaExecRow;
import com.rxas400adm.as400.model.SpoolRow;

import java.util.ArrayList;
import java.util.List;

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
}