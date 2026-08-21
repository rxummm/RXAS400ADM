package com.rxas400adm.monitor.service;

import com.rxas400adm.monitor.mapper.MetricMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapacityServiceTest {

    private final MetricMapper metricMapper = mock(MetricMapper.class);

    private CapacityService service = new CapacityService(metricMapper);

    private List<Map<String, Object>> risingDiskAggregates() {
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate base = LocalDate.now().minusDays(30);
        for (int day = 0; day < 30; day++) {
            double value = 40 + day * 1.0; // 单调上升但 30 天内未达 90
            list.add(Map.of("day", base.plusDays(day), "avgValue", value, "maxValue", value + 1));
        }
        return list;
    }

    @Test
    void trend_shouldAggregateByDayAndPredict() {
        when(metricMapper.selectDailyDiskAggregates(eq(1L), any(LocalDateTime.class)))
                .thenReturn(risingDiskAggregates());
        Map<String, Object> result = service.trend(1L, 30);

        List<?> points = (List<?>) result.get("points");
        assertEquals(30, points.size());
        Map<?, ?> first = (Map<?, ?>) points.get(0);
        assertTrue(((Number) first.get("avg")).doubleValue() > 0);

        List<?> prediction = (List<?>) result.get("prediction");
        assertTrue(prediction.size() == CapacityService.PREDICT_DAYS);
        assertNotNull(result.get("daysToThreshold"));
    }

    @Test
    void trend_noData_shouldReturnEmptyPoints() {
        when(metricMapper.selectDailyDiskAggregates(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of());
        Map<String, Object> result = service.trend(1L, 30);
        assertEquals(0, ((List<?>) result.get("points")).size());
        assertEquals(0, ((List<?>) result.get("prediction")).size());
        assertEquals(null, result.get("daysToThreshold"));
    }

    @Test
    void trend_fewPoints_shouldSkipPrediction() {
        List<Map<String, Object>> few = List.of(
                Map.of("day", LocalDate.now().minusDays(2), "avgValue", 50.0, "maxValue", 51.0),
                Map.of("day", LocalDate.now().minusDays(1), "avgValue", 52.0, "maxValue", 53.0)
        );
        when(metricMapper.selectDailyDiskAggregates(eq(1L), any(LocalDateTime.class))).thenReturn(few);
        Map<String, Object> result = service.trend(1L, 30);
        assertEquals(2, ((List<?>) result.get("points")).size());
        assertEquals(0, ((List<?>) result.get("prediction")).size());
    }
}