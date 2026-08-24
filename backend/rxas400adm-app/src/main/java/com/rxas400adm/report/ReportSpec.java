package com.rxas400adm.report;

import java.util.List;

/**
 * 报表标题与表头的唯一事实源（中-17 去重）：此前 ReportService.generate 与
 * ReportController 三个端点各写一份 title/headers 字面量，漂移风险高。
 * 两处一律经由本类工厂方法取用。
 */
record ReportSpec(String title, List<String> headers) {

    String[] headerArray() {
        return headers.toArray(String[]::new);
    }

    static ReportSpec metrics(Long instanceId, int days) {
        return new ReportSpec("IBM i 指标报表（instance=" + instanceId + " 近" + days + "天）",
                List.of("date", "metric", "avg", "max", "min", "samples"));
    }

    static ReportSpec executions() {
        return new ReportSpec("执行记录报表",
                List.of("time", "source", "name", "type", "user", "server", "status", "message", "costMs"));
    }

    static ReportSpec capacity(Long instanceId) {
        return new ReportSpec("磁盘容量趋势报表（instance=" + instanceId + "）",
                List.of("kind", "date", "avg", "max"));
    }
}
