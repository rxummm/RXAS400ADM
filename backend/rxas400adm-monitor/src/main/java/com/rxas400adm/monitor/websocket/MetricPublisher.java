package com.rxas400adm.monitor.websocket;

import com.rxas400adm.monitor.domain.Metric;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 实时指标推送：Collector → MetricService → MetricPublisher → WebSocket → Vue Chart
 * 订阅地址：/topic/monitor/{instanceId}
 */
@Component
@RequiredArgsConstructor
public class MetricPublisher {

    private final SimpMessagingTemplate template;

    public void publish(Metric metric) {
        template.convertAndSend("/topic/monitor/" + metric.getInstanceId(), metric);
    }
}
