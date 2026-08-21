package com.rxas400adm.monitor.collector;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.monitor.domain.Metric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * CPU 采集：SELECT CPU_UTILIZATION FROM QSYS2.SYSTEM_STATUS_INFO
 */
@Component
@RequiredArgsConstructor
public class CpuCollector implements MetricCollector {

    private final AS400ClientProvider clientProvider;

    @Override
    public String name() {
        return "CPU";
    }

    @Override
    public Metric collect(Long instanceId) {
        AS400Client client = clientProvider.forServer(instanceId);
        Map<String, Object> row = client.querySingle(
                "SELECT CPU_UTILIZATION FROM QSYS2.SYSTEM_STATUS_INFO");
        Double value = toDouble(row.get("CPU_UTILIZATION"));
        return Metric.builder()
                .instanceId(instanceId)
                .metricType("SYSTEM")
                .metricName("CPU")
                .metricValue(value == null ? 0.0 : value)
                .collectTime(LocalDateTime.now())
                .build();
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
            return null;
        }
    }
}
