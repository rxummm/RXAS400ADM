package com.rxas400adm.monitor.collector;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.monitor.domain.Metric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Disk ASP 采集：SELECT ASP_NAME, TOTAL_SPACE, USED_SPACE FROM QSYS2.ASP_INFO
 * 取 SYSBAS 的使用率（%），无真实数据时返回 0（优雅降级）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiskCollector implements MetricCollector {

    private final AS400ClientProvider clientProvider;

    @Override
    public String name() {
        return "DISK";
    }

    @Override
    public Metric collect(Long instanceId) {
        AS400Client client = clientProvider.forServer(instanceId);
        List<Map<String, Object>> rows = client.queryList(
                "SELECT ASP_NAME, TOTAL_SPACE, USED_SPACE FROM QSYS2.ASP_INFO");
        double usedPercent = 0.0;
        for (Map<String, Object> row : rows) {
            if ("SYSBAS".equalsIgnoreCase(String.valueOf(row.get("ASP_NAME")))) {
                usedPercent = percent(row);
                break;
            }
        }
        return Metric.builder()
                .instanceId(instanceId)
                .metricType("SYSTEM")
                .metricName("DISK")
                .metricValue(usedPercent)
                .collectTime(LocalDateTime.now())
                .build();
    }

    /**
     * 兼容 Number / String 两种返回类型（JT400 JDBC 数字列通常为 BigDecimal，
     * mock 或其它驱动可能返回 String）。
     */
    private double percent(Map<String, Object> row) {
        Double total = toDouble(row.get("TOTAL_SPACE"));
        Double used = toDouble(row.get("USED_SPACE"));
        if (total == null || used == null || total <= 0) {
            return 0.0;
        }
        return Math.round(used / total * 1000) / 10.0;
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            log.warn("Disk ASP 数值解析失败: {} (值={})", e.getMessage(), value);
            return null;
        }
    }
}
