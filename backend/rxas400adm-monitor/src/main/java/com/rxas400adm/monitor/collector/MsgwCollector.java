package com.rxas400adm.monitor.collector;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.monitor.domain.Metric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MSGW 作业计数采集：SELECT ... FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) WHERE JOB_STATUS = 'MSGW'
 * 生产环境中 MSGW 数量是最重要的告警信号之一。
 */
@Component
@RequiredArgsConstructor
public class MsgwCollector implements MetricCollector {

    private final AS400ClientProvider clientProvider;

    @Override
    public String name() {
        return "MSGW";
    }

    @Override
    public Metric collect(Long instanceId) {
        AS400Client client = clientProvider.forServer(instanceId);
        int count = client.queryList(
                SqlStatementRegistry.of("monitor.msgw.count")).size();
        return Metric.builder()
                .instanceId(instanceId)
                .metricType("JOB")
                .metricName("MSGW")
                .metricValue((double) count)
                .collectTime(LocalDateTime.now())
                .build();
    }
}
