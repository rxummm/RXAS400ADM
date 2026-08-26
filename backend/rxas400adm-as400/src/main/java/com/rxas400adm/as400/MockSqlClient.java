package com.rxas400adm.as400;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mock SqlClient 委托实现（SQL 查询仿真路由，含活动作业/ASP/子系统/作业日志/消息队列/业务表）。
 * queryList 的关键字路由收敛为 {@link LinkedHashMap} 路由表（中-4），保持原 if 链的
 * contains 匹配顺序语义；兜底仍是业务表 SELECT 模拟。
 */
class MockSqlClient implements SqlClient {

    /** 关键字 → 行生成函数；遍历顺序即匹配优先级 */
    @FunctionalInterface
    private interface SqlRows {
        List<Map<String, Object>> rows(String upperSql);
    }

    private final MockState state;
    private final Map<String, SqlRows> listRoutes;

    MockSqlClient(MockState state) {
        this.state = state;
        this.listRoutes = new LinkedHashMap<>();
        this.listRoutes.put("ACTIVE_JOB_INFO", this::mockActiveJobs);
        this.listRoutes.put("ASP_INFO", sql -> mockAspNet());
        this.listRoutes.put("SUBSYSTEM_INFO", sql -> List.of(
                MockState.row("SUBSYSTEM_NAME", "QINTER", "STATUS", "ACTIVE"),
                MockState.row("SUBSYSTEM_NAME", "QBATCH", "STATUS", "ACTIVE"),
                MockState.row("SUBSYSTEM_NAME", "QHTTPSVR", "STATUS", "ACTIVE"),
                MockState.row("SUBSYSTEM_NAME", "QUSRWRK", "STATUS", "ACTIVE")));
        this.listRoutes.put("NETSTAT_INFO", sql -> List.of(
                MockState.row("LOCAL_ADDRESS", "10.1.1.10", "REMOTE_ADDRESS", "10.2.3.4", "STATE", "ESTABLISHED"),
                MockState.row("LOCAL_ADDRESS", "10.1.1.10", "REMOTE_ADDRESS", "10.5.6.7", "STATE", "ESTABLISHED"),
                MockState.row("LOCAL_ADDRESS", "10.1.1.10", "REMOTE_ADDRESS", "10.5.6.7", "STATE", "LISTEN")));
        this.listRoutes.put("OUTPUT_QUEUE_INFO", sql -> List.of(
                MockState.row("OUTPUT_QUEUE", "QPRINT", "OUTPUT_QUEUE_LIBRARY", "QUSRSYS", "SPOOLED_FILE_STATUS", "READY"),
                MockState.row("OUTPUT_QUEUE", "QPRINT", "OUTPUT_QUEUE_LIBRARY", "QUSRSYS", "SPOOLED_FILE_STATUS", "HELD"),
                MockState.row("OUTPUT_QUEUE", "QPRINT", "OUTPUT_QUEUE_LIBRARY", "QUSRSYS", "SPOOLED_FILE_STATUS", "READY"),
                MockState.row("OUTPUT_QUEUE", "QSYSPRT", "OUTPUT_QUEUE_LIBRARY", "QUSRSYS", "SPOOLED_FILE_STATUS", "READY")));
        this.listRoutes.put("JOBLOG_INFO", sql -> mockJobLog());
        this.listRoutes.put("MESSAGE_QUEUE_INFO", sql -> List.of(
                MockState.row("MESSAGE_ID", "CPA0701", "MESSAGE_TYPE", "INQUIRY",
                        "MESSAGE_TEXT", "Reply to message (CPA0701)", "REPLY_STATUS", "MSGW"),
                MockState.row("MESSAGE_ID", "CPF1241", "MESSAGE_TYPE", "INFORMATIONAL",
                        "MESSAGE_TEXT", "Job ended normally", "REPLY_STATUS", "")));
        this.listRoutes.put("SYSTABLES", this::mockSysTables);
        this.listRoutes.put("SYSCOLUMNS", this::mockSysColumns);
    }

    private double fluctuate(double base, double amplitude) {
        double wave = Math.sin(LocalTime.now().toSecondOfDay() / 30.0 + state.serverName.hashCode() % 7)
                * amplitude + MockState.RANDOM.nextDouble() * amplitude * 0.5;
        return Math.round((base + wave) * 10) / 10.0;
    }

    @Override
    public Map<String, Object> querySingle(String sql) {
        String upper = sql.toUpperCase();
        if (upper.contains("CPU_UTILIZATION")) {
            double value = MockState.RANDOM.nextDouble() < 0.03
                    ? 82.0 + MockState.RANDOM.nextDouble() * 6.0
                    : fluctuate(62.0, 6.0);
            return Map.of("CPU_UTILIZATION", Math.round(value * 10) / 10.0);
        }
        if (upper.contains("MAIN_STORAGE_USED_PERCENT")) {
            return Map.of("MAIN_STORAGE_USED_PERCENT", fluctuate(64.0, 6.0));
        }
        return Map.of("RESULT", "MOCK");
    }

    @Override
    public List<Map<String, Object>> queryList(String sql) {
        String upper = sql.toUpperCase();
        for (Map.Entry<String, SqlRows> route : listRoutes.entrySet()) {
            if (upper.contains(route.getKey())) {
                return route.getValue().rows(upper);
            }
        }
        List<Map<String, Object>> tableRows = mockSelectAll(upper);
        if (tableRows != null) {
            return tableRows;
        }
        return List.of();
    }

    @Override
    public List<Map<String, Object>> queryList(String sql, Object... params) {
        if (params == null || params.length == 0) {
            return queryList(sql);
        }
        return queryList(substitute(sql, params));
    }

    @Override
    public List<Map<String, Object>> queryListChecked(String sql, Object... params) {
        return queryList(sql, params);
    }

    private static String substitute(String sql, Object... params) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (Object param : params) {
            int at = sql.indexOf('?', idx);
            if (at < 0) {
                break;
            }
            sb.append(sql, idx, at);
            sb.append('\'').append(String.valueOf(param).replace("'", "''")).append('\'');
            idx = at + 1;
        }
        sb.append(sql.substring(idx));
        return sb.toString();
    }

    private List<Map<String, Object>> mockSysTables(String sql) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String keyword = extractLike(sql);
        for (Map.Entry<String, String> e : MockState.MOCK_TABLE_TEXT.entrySet()) {
            if (keyword == null || e.getKey().contains(keyword)) {
                rows.add(MockState.row("TABLE_SCHEMA", "APP", "TABLE_NAME", e.getKey(),
                        "TABLE_TEXT", e.getValue()));
            }
        }
        rows.add(MockState.row("TABLE_SCHEMA", "APP", "TABLE_NAME", "INVENTORY", "TABLE_TEXT", "库存台账"));
        return rows;
    }

    private List<Map<String, Object>> mockSysColumns(String sql) {
        String table = extractTableName(sql);
        return switch (table == null ? "" : table) {
            case "ORDERS" -> List.of(
                    col(1, "ORDER_NO", "CHARACTER", 20, 0, "N", "订单号"),
                    col(2, "CUSTOMER_NAME", "VARCHAR", 60, 0, "N", "客户名称"),
                    col(3, "ORDER_DATE", "DATE", 10, 0, "N", "下单日期"),
                    col(4, "AMOUNT", "DECIMAL", 15, 2, "N", "订单金额"),
                    col(5, "STATUS", "CHARACTER", 10, 0, "Y", "状态"),
                    col(6, "SKU_COUNT", "INTEGER", 10, 0, "Y", "商品件数"));
            case "SKU_MASTER" -> List.of(
                    col(1, "SKU_CODE", "CHARACTER", 15, 0, "N", "SKU 编码"),
                    col(2, "SKU_NAME", "VARCHAR", 80, 0, "N", "商品名称"),
                    col(3, "CATEGORY", "CHARACTER", 20, 0, "Y", "分类"),
                    col(4, "UNIT_PRICE", "DECIMAL", 12, 2, "Y", "单价"),
                    col(5, "STOCK_QTY", "INTEGER", 10, 0, "Y", "库存数量"),
                    col(6, "STATUS", "CHARACTER", 1, 0, "Y", "状态"));
            case "PRICE_MASTER" -> List.of(
                    col(1, "SKU_CODE", "CHARACTER", 15, 0, "N", "SKU 编码"),
                    col(2, "PRICE_TYPE", "CHARACTER", 10, 0, "N", "价格类型"),
                    col(3, "PRICE", "DECIMAL", 12, 2, "N", "价格"),
                    col(4, "EFFECTIVE_DATE", "DATE", 10, 0, "N", "生效日期"),
                    col(5, "EXPIRE_DATE", "DATE", 10, 0, "Y", "失效日期"));
            case "CUSTMAST" -> List.of(
                    col(1, "CUSTNO", "NUMERIC", 7, 0, "N", "客户编号"),
                    col(2, "CUSTNAME", "CHARACTER", 30, 0, "N", "客户名称"),
                    col(3, "BALANCE", "DECIMAL", 12, 2, "Y", "账户余额"));
            default -> List.of(
                    col(1, "FIELD1", "CHARACTER", 20, 0, "Y", "字段一"),
                    col(2, "FIELD2", "DECIMAL", 10, 2, "Y", "字段二"));
        };
    }

    private Map<String, Object> col(int ordinal, String name, String type, int len, int scale,
                                    String nullable, String text) {
        return MockState.row("ORDINAL_POSITION", ordinal, "COLUMN_NAME", name, "DATA_TYPE", type,
                "LENGTH", len, "SCALE", scale, "IS_NULLABLE", nullable,
                "COLUMN_TEXT", text, "COLUMN_DEFAULT", null);
    }

    private List<Map<String, Object>> mockSelectAll(String sql) {
        String table = extractTableName(sql);
        if (table == null) {
            return null;
        }
        String keyword = extractLike(sql);
        List<Map<String, Object>> rows = mockBizRows(table, keyword);
        if (sql.toUpperCase().startsWith("SELECT COUNT(*)")) {
            return List.of(MockState.row("CNT", (long) rows.size()));
        }
        Matcher page = Pattern.compile(
                "OFFSET\\s+(\\d+)\\s+ROWS\\s+FETCH\\s+NEXT\\s+(\\d+)\\s+ROWS",
                Pattern.CASE_INSENSITIVE).matcher(sql);
        if (page.find()) {
            int offset = Integer.parseInt(page.group(1));
            int limit = Integer.parseInt(page.group(2));
            if (offset >= rows.size()) {
                return List.of();
            }
            return rows.subList(offset, Math.min(offset + limit, rows.size()));
        }
        return rows;
    }

    private List<Map<String, Object>> mockBizRows(String table, String keyword) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String[] customers = {"华东贸易", "百汇商贸", "远方物流", "恒丰实业", "锐达电子", "蓝湾科技"};
        String[] statuses = {"COMPLETED", "OPEN", "CANCELLED", "OPEN"};
        for (int i = 1; i <= 60; i++) {
            Map<String, Object> r = switch (table) {
                case "ORDERS" -> MockState.row("ORDER_NO", "SO" + String.format("%06d", i),
                        "CUSTOMER_NAME", customers[i % customers.length],
                        "ORDER_DATE", "2026-0" + (i % 9 + 1) + "-" + String.format("%02d", i % 28 + 1),
                        "AMOUNT", (i * 137) % 10000 + i,
                        "STATUS", statuses[i % statuses.length],
                        "SKU_COUNT", i % 8 + 1);
                case "SKU_MASTER" -> MockState.row("SKU_CODE", "SKU" + String.format("%05d", i),
                        "SKU_NAME", "商品" + (i + 100),
                        "CATEGORY", i % 3 == 0 ? "电子" : i % 3 == 1 ? "服装" : "家居",
                        "UNIT_PRICE", (i * 37) % 500 + 10,
                        "STOCK_QTY", (i * 53) % 200,
                        "STATUS", i % 5 == 0 ? "D" : "A");
                case "PRICE_MASTER" -> MockState.row("SKU_CODE", "SKU" + String.format("%05d", i),
                        "PRICE_TYPE", i % 3 == 0 ? "RETAIL" : i % 3 == 1 ? "WHOLESALE" : "PROMOTION",
                        "PRICE", (i * 37) % 500 + 10,
                        "EFFECTIVE_DATE", "2026-0" + (i % 9 + 1) + "-01",
                        "EXPIRE_DATE", "2027-" + String.format("%02d", i % 12 + 1) + "-01");
                case "CUSTMAST" -> MockState.row("CUSTNO", 10000 + i,
                        "CUSTNAME", "客户" + (100 + i),
                        "BALANCE", (i * 137) % 10000);
                default -> null;
            };
            if (r != null) {
                rows.add(r);
            }
        }
        if (keyword == null) {
            return rows;
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            boolean match = r.values().stream()
                    .anyMatch(v -> v != null && String.valueOf(v).toUpperCase().contains(keyword));
            if (match) {
                filtered.add(r);
            }
        }
        return filtered;
    }

    private String extractTableName(String sql) {
        Matcher m =
                Pattern.compile("TABLE_NAME\\s*=\\s*'([^']+)'",
                        Pattern.CASE_INSENSITIVE).matcher(sql);
        if (m.find()) {
            return m.group(1).toUpperCase();
        }
        m = Pattern.compile("FROM\\s+(?:[A-Z0-9_$#@]+\\.)?([A-Z0-9_$#@]+)\\s*(?:WHERE|OFFSET|ORDER|FETCH)?",
                Pattern.CASE_INSENSITIVE).matcher(sql);
        if (m.find() && !"QSYS2".equalsIgnoreCase(m.group(1))) {
            String t = m.group(1).toUpperCase();
            if (MockState.MOCK_TABLE_TEXT.containsKey(t)) {
                return t;
            }
        }
        return null;
    }

    private String extractLike(String sql) {
        Matcher m = Pattern.compile(
                "LIKE\\s*'%([^']+)%'", Pattern.CASE_INSENSITIVE).matcher(sql);
        return m.find() ? m.group(1).toUpperCase() : null;
    }

    private List<Map<String, Object>> mockJobLog() {
        String[] types = {"INFORMATIONAL", "DIAGNOSTIC", "ESCAPE", "INQUIRY"};
        String[] texts = {"Job started", "Program QRPGLE member started", "Compile completed successfully",
                "File not found - retry or reply", "User not authorized", "Job ended"};
        List<Map<String, Object>> rows = new ArrayList<>();
        int count = 6 + MockState.RANDOM.nextInt(6);
        for (int i = 1; i <= count; i++) {
            rows.add(MockState.row("ORDINAL_POSITION", i, "MESSAGE_ID", "CPF" + (1000 + MockState.RANDOM.nextInt(8999)),
                    "MESSAGE_TYPE", types[MockState.RANDOM.nextInt(types.length)],
                    "MESSAGE_TEXT", texts[MockState.RANDOM.nextInt(texts.length)],
                    "MESSAGE_TIMESTAMP", "2026-08-12 10:" + String.format("%02d", i * 2) + ":00"));
        }
        return rows;
    }

    private List<Map<String, Object>> mockActiveJobs(String sql) {
        String[] users = {"QSECOFR", "QPGMR", "ADMIN", "DEVELOPER", "OPERATOR", "QUSER"};
        String[] programs = {"QRPGLESRC", "QCLSRC", "QCMDB", "QZRCSRVS", "QZDASOINIT", "QDBFSTC"};
        boolean filterMsgw = sql.contains("'MSGW'") || sql.contains("\"MSGW\"");
        boolean filterLckw = sql.contains("'LCKW'") || sql.contains("\"LCKW\"");

        List<Map<String, Object>> jobs = new ArrayList<>();
        int total = 8 + MockState.RANDOM.nextInt(6);
        for (int i = 0; i < total; i++) {
            String status = "RUN";
            if (i % 5 == 0) {
                status = "MSGW";
            } else if (i % 7 == 0) {
                status = "LCKW";
            } else if (i % 4 == 0) {
                status = "ACTIVE";
            } else if (i % 3 == 0) {
                status = "JOBQ";
            }
            if (filterMsgw && !"MSGW".equals(status)) {
                continue;
            }
            if (filterLckw && !"LCKW".equals(status)) {
                continue;
            }
            Map<String, Object> job = new LinkedHashMap<>();
            job.put("JOB_NAME", String.format("JOB%04d", 100000 + MockState.RANDOM.nextInt(89999)));
            job.put("JOB_USER", users[MockState.RANDOM.nextInt(users.length)]);
            job.put("JOB_NUMBER", String.valueOf(100000 + MockState.RANDOM.nextInt(899999)));
            job.put("JOB_STATUS", status);
            job.put("JOB_PROGRAM", programs[MockState.RANDOM.nextInt(programs.length)]);
            job.put("CPU_TIME", MockState.RANDOM.nextInt(600) + 1);
            job.put("TEMPORARY_STORAGE", MockState.RANDOM.nextInt(20000) + 100);
            jobs.add(job);
        }
        return jobs;
    }

    private List<Map<String, Object>> mockAspNet() {
        return List.of(
                MockState.row("ASP_NAME", "SYSBAS", "TOTAL_SPACE", 1048576.0, "USED_SPACE", fluctuate(78.0, 4.0) / 100 * 1048576.0),
                MockState.row("ASP_NAME", "ASP01", "TOTAL_SPACE", 524288.0, "USED_SPACE", fluctuate(52.0, 5.0) / 100 * 524288.0));
    }
}