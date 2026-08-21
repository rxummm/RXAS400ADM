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
 * 网络采集（2.1.6，M6）：QSYS2.NETSTAT_INFO 的活动连接数，无真实数据返回 0。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NetworkCollector implements MetricCollector {

    private final AS400ClientProvider clientProvider;

    @Override
    public String name() {
        return "NETWORK";
    }

    @Override
    public Metric collect(Long instanceId) {
        AS400Client client = clientProvider.forServer(instanceId);
        List<Map<String, Object>> rows = client.queryList(
                "SELECT LOCAL_ADDRESS, REMOTE_ADDRESS, STATE FROM QSYS2.NETSTAT_INFO");
        long connections = rows.stream()
                .filter(r -> "ESTABLISHED".equalsIgnoreCase(String.valueOf(r.get("STATE"))))
                .count();
        return Metric.builder()
                .instanceId(instanceId)
                .metricType("SYSTEM")
                .metricName("NETWORK")
                .metricValue((double) connections)
                .collectTime(LocalDateTime.now())
                .build();
    }
}
