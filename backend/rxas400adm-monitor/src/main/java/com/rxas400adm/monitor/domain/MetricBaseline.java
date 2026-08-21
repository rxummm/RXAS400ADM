package com.rxas400adm.monitor.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 性能基线（rx_metric_baseline，2.1.9）：每服务器每指标每日均值/峰值/最小值。
 */
@Data
@TableName("rx_metric_baseline")
public class MetricBaseline {

    private Long instanceId;

    private String metricName;

    private LocalDate baselineDate;

    private Double avgValue;

    private Double maxValue;

    private Double minValue;

    private Integer sampleCount;
}
