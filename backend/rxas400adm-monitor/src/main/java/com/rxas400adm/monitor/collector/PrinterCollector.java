package com.rxas400adm.monitor.collector;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.monitor.domain.Metric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 打印/SPOOL 采集（2.1.6，M7）：QSYS2.OUTPUT_QUEUE_INFO 的输出队列待处理文件数，无真实数据返回 0。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrinterCollector implements MetricCollector {

    private final AS400ClientProvider clientProvider;

    @Override
    public String name() {
        return "PRINTER";
    }

    @Override
    public Metric collect(Long instanceId) {
        AS400Client client = clientProvider.forServer(instanceId);
        List<Map<String, Object>> rows = client.queryList(
                SqlStatementRegistry.of("monitor.printer.spool"));
        return Metric.builder()
                .instanceId(instanceId)
                .metricType("SYSTEM")
                .metricName("PRINTER")
                .metricValue((double) rows.size())
                .collectTime(LocalDateTime.now())
                .build();
    }
}
