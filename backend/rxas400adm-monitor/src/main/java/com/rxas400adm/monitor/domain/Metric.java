package com.rxas400adm.monitor.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指标表（rx_metric），生产环境建议转为 TimescaleDB hypertable
 */
@Data
@Builder
@TableName("rx_metric")
public class Metric {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;

    private String metricType;

    private String metricName;

    private Double metricValue;

    private LocalDateTime collectTime;
}
