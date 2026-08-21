package com.rxas400adm.monitor.collector;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.monitor.domain.Metric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * LCKW 作业计数采集：SELECT ... FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) WHERE JOB_STATUS = 'LCKW'
 * 用于发现锁等待 / 死锁风险。
 */
@Component
@RequiredArgsConstructor
public class LckwCollector implements MetricCollector {

    private final AS400ClientProvider clientProvider;

    @Override
    public String name() {
        return "LCKW";
    }

    @Override
    public Metric collect(Long instanceId) {
        AS400Client client = clientProvider.forServer(instanceId);
        int count = client.queryList(
                "SELECT JOB_NAME FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) X WHERE JOB_STATUS = 'LCKW'").size();
        return Metric.builder()
                .instanceId(instanceId)
                .metricType("JOB")
                .metricName("LCKW")
                .metricValue((double) count)
                .collectTime(LocalDateTime.now())
                .build();
    }
}
