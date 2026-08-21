package com.rxas400adm.monitor.vo;

import com.rxas400adm.monitor.domain.Metric;

import java.time.LocalDateTime;

/**
 * 指标采样视图（P3-9）：与 Metric 字段契约解耦，供监控页面/图表消费。
 * 剔除内部自增 id，只保留采样所需字段，避免表结构演进时内部字段泄漏到 API。
 */
public record MetricVO(
        Long instanceId,
        String metricType,
        String metricName,
        Double metricValue,
        LocalDateTime collectTime) {

    public static MetricVO from(Metric m) {
        return new MetricVO(m.getInstanceId(), m.getMetricType(), m.getMetricName(),
                m.getMetricValue(), m.getCollectTime());
    }
}