package com.rxas400adm.monitor.collector;

import com.rxas400adm.monitor.domain.Metric;

/**
 * 指标采集器统一接口，新增指标只需新增一个 Collector 实现并注册为 Bean。
 */
public interface MetricCollector {

    String name();

    Metric collect(Long instanceId);
}
