package com.rxas400adm.monitor.service;

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
 * 容量规划（2.3.6）：基于已采集的 DISK 指标（rx_metric），
 * 输出每日均值/峰值趋势 + 线性回归预测未来 30 天 + 预估达到阈值的剩余天数。
 */
@Service
@RequiredArgsConstructor
public class CapacityService implements ICapacityService {

    /** 容量预警阈值（%），预测交叉该值即报告剩余天数 */
    static final double WARN_THRESHOLD = 90.0;
    static final int PREDICT_DAYS = 30;

    private final MetricMapper metricMapper;

    public Map<String, Object> trend(Long instanceId, int days) {
        int window = Math.max(7, Math.min(days, 365));
        LocalDateTime since = LocalDateTime.now().minusDays(window);

        // 按天聚合（GROUP BY 下推，MetricMapper.xml）：返回 {day, avgValue, maxValue} 升序
        List<Map<String, Object>> aggregates = metricMapper.selectDailyDiskAggregates(instanceId, since);

        List<Map<String, Object>> points = new ArrayList<>();
        List<Double> avgs = new ArrayList<>();
        for (Map<String, Object> agg : aggregates) {
            double avg = ((Number) agg.get("avgValue")).doubleValue();
            double max = ((Number) agg.get("maxValue")).doubleValue();
            avgs.add(avg);
            points.add(Map.of("date", agg.get("day").toString(), "avg", round(avg), "max", round(max)));
        }

        // 线性回归预测（至少 3 个数据点才有意义）
        List<Map<String, Object>> prediction = new ArrayList<>();
        Integer daysToThreshold = null;
        if (avgs.size() >= 3) {
            double[] slopeIntercept = linearRegression(avgs);
            double slope = slopeIntercept[0];
            double intercept = slopeIntercept[1];
            LocalDate last = LocalDate.parse(aggregates.get(aggregates.size() - 1).get("day").toString());
            for (int i = 1; i <= PREDICT_DAYS; i++) {
                double predicted = slope * (avgs.size() - 1 + i) + intercept;
                prediction.add(Map.of("date", last.plusDays(i).toString(), "value", round(Math.max(0, predicted))));
            }
            if (slope > 0) {
                // 求预测值到达阈值的天数
                double target = (WARN_THRESHOLD - intercept) / slope - (avgs.size() - 1);
                if (target > 0 && target <= 3650) {
                    daysToThreshold = (int) Math.ceil(target);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("points", points);
        result.put("prediction", prediction);
        result.put("daysToThreshold", daysToThreshold);
        result.put("threshold", WARN_THRESHOLD);
        return result;
    }

    /** 最小二乘线性回归：返回 [slope, intercept]（x 为 0,1,2,...） */
    private double[] linearRegression(List<Double> y) {
        int n = y.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += y.get(i);
            sumXY += (double) i * y.get(i);
            sumXX += (double) i * i;
        }
        double denom = n * sumXX - sumX * sumX;
        double slope = denom == 0 ? 0 : (n * sumXY - sumX * sumY) / denom;
        double intercept = denom == 0 ? (n == 0 ? 0 : sumY / n) : (sumY - slope * sumX) / n;
        return new double[]{slope, intercept};
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}