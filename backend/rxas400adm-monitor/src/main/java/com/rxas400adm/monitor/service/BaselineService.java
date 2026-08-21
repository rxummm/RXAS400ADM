package com.rxas400adm.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.monitor.domain.Metric;
import com.rxas400adm.monitor.domain.MetricBaseline;
import com.rxas400adm.monitor.mapper.MetricBaselineMapper;
import com.rxas400adm.monitor.mapper.MetricMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 性能基线（2.1.9，参照旧项目 As400PerformanceBaselineService）：\n * 汇总近 7 天各指标为日均基线（rx_metric_baseline），并计算当前值相对基线的偏差百分比。\n */
@Service
@RequiredArgsConstructor
public class BaselineService implements IBaselineService {

    private static final int WINDOW_DAYS = 7;
    private static final List<String> METRICS = List.of("CPU", "MEMORY", "DISK", "NETWORK", "PRINTER");

    private final MetricMapper metricMapper;
    private final MetricBaselineMapper baselineMapper;

    /** 计算并保存近 7 天基线（幂等 upsert） */
    
    public void computeBaseline(Long instanceId) {
        LocalDateTime since = LocalDateTime.now().minusDays(WINDOW_DAYS);
        List<Map<String, Object>> aggregates = metricMapper.selectBaselineAggregates(instanceId, since);

        LocalDate today = LocalDate.now();
        for (Map<String, Object> agg : aggregates) {
            long sampleCount = ((Number) agg.get("sampleCount")).longValue();
            if (sampleCount < 3) {
                continue;
            }
            double avg = ((Number) agg.get("avgValue")).doubleValue();
            double max = ((Number) agg.get("maxValue")).doubleValue();
            double min = ((Number) agg.get("minValue")).doubleValue();
            upsert(instanceId, (String) agg.get("metricName"), today, avg, max, min, (int) sampleCount);
        }
    }

    /** 最近基线 + 当前值 + 偏差（%），供前端展示 */ 
    public List<Map<String, Object>> baselineWithCurrent(Long instanceId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String metric : METRICS) {
            MetricBaseline baseline = baselineMapper.selectOne(new LambdaQueryWrapper<MetricBaseline>()
                    .eq(MetricBaseline::getInstanceId, instanceId)
                    .eq(MetricBaseline::getMetricName, metric)
                    .orderByDesc(MetricBaseline::getBaselineDate)
                    .last(PageConstants.limitClause(1)));
            if (baseline == null) {
                continue;
            }
            Metric current = latest(instanceId, metric);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("metric", metric);
            row.put("baselineDate", baseline.getBaselineDate());
            row.put("avg", safeRound(baseline.getAvgValue()));
            row.put("max", safeRound(baseline.getMaxValue()));
            row.put("min", safeRound(baseline.getMinValue()));
            row.put("sampleCount", baseline.getSampleCount());
            double currentValue = current == null ? Double.NaN : current.getMetricValue();
            row.put("current", current == null ? null : round(currentValue));
            if (current != null && baseline.getAvgValue() > 0) {
                double deviation = (currentValue - baseline.getAvgValue()) / baseline.getAvgValue() * 100;
                row.put("deviation", round(deviation));
            } else {
                row.put("deviation", null);
            }
            result.add(row);
        }
        return result;
    }

    private Metric latest(Long instanceId, String metricName) {
        List<Metric> list = metricMapper.selectList(new LambdaQueryWrapper<Metric>()
                .eq(Metric::getInstanceId, instanceId)
                .eq(Metric::getMetricName, metricName)
                .orderByDesc(Metric::getCollectTime)
                .last(PageConstants.limitClause(1)));
        return list.isEmpty() ? null : list.get(0);
    }

    private void upsert(Long instanceId, String metric, LocalDate date,
                        double avg, double max, double min, int count) {
        MetricBaseline existing = baselineMapper.selectOne(new LambdaQueryWrapper<MetricBaseline>()
                .eq(MetricBaseline::getInstanceId, instanceId)
                .eq(MetricBaseline::getMetricName, metric)
                .eq(MetricBaseline::getBaselineDate, date));
        MetricBaseline baseline = existing == null ? new MetricBaseline() : existing;
        baseline.setInstanceId(instanceId);
        baseline.setMetricName(metric);
        baseline.setBaselineDate(date);
        baseline.setAvgValue(avg);
        baseline.setMaxValue(max);
        baseline.setMinValue(min);
        baseline.setSampleCount(count);
        if (existing == null) {
            baselineMapper.insert(baseline);
        } else {
            baselineMapper.updateById(baseline);
        }
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private Double safeRound(Double v) {
        return v == null ? null : round(v);
    }
}