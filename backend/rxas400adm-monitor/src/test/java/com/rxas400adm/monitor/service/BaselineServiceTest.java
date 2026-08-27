package com.rxas400adm.monitor.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.rxas400adm.monitor.domain.Metric;
import com.rxas400adm.monitor.domain.MetricBaseline;
import com.rxas400adm.monitor.mapper.MetricBaselineMapper;
import com.rxas400adm.monitor.mapper.MetricMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BaselineServiceTest {

    @Mock
    private MetricMapper metricMapper;

    @Mock
    private MetricBaselineMapper baselineMapper;

    @Test
    void compute_shouldUpsertBaselineForEnoughSamples() {
        // CPU 7 样本满足 >=3；DISK 2 样本不足跳过
        when(metricMapper.selectBaselineAggregates(eq(1L), any(LocalDateTime.class))).thenReturn(List.of(
                Map.of("metricName", "CPU", "avgValue", 53.0, "maxValue", 56.0, "minValue", 50.0, "sampleCount", 7),
                Map.of("metricName", "DISK", "avgValue", 50.5, "maxValue", 51.0, "minValue", 50.0, "sampleCount", 2)));
        when(baselineMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        BaselineService service = new BaselineService(metricMapper, baselineMapper);
        service.computeBaseline(1L);

        ArgumentCaptor<MetricBaseline> captor = ArgumentCaptor.forClass(MetricBaseline.class);
        verify(baselineMapper).insert(captor.capture());
        verify(baselineMapper, never()).updateById(any(MetricBaseline.class));
        // 只有 CPU 满足 >=3 样本
        assertEquals("CPU", captor.getValue().getMetricName());
        assertEquals(7, captor.getValue().getSampleCount());
    }

    @Test
    void baselineWithCurrent_shouldReturnDeviation() {
        MetricBaseline baseline = new MetricBaseline();
        baseline.setInstanceId(1L);
        baseline.setMetricName("CPU");
        baseline.setBaselineDate(LocalDate.now());
        baseline.setAvgValue(50.0);
        baseline.setMaxValue(60.0);
        baseline.setMinValue(40.0);
        baseline.setSampleCount(7);
        when(baselineMapper.selectOne(any(Wrapper.class)))
                .thenReturn(baseline, null, null, null, null); // 仅 CPU 有基线
        when(metricMapper.selectList(any(Wrapper.class))).thenReturn(
                List.of(Metric.builder().instanceId(1L).metricName("CPU")
                        .metricValue(60.0).collectTime(LocalDateTime.now()).build()));

        BaselineService service = new BaselineService(metricMapper, baselineMapper);
        List<Map<String, Object>> rows = service.baselineWithCurrent(1L);

        assertEquals(1, rows.size());
        Map<String, Object> row = rows.get(0);
        assertEquals("CPU", row.get("metric"));
        assertNotNull(row.get("current"));
        assertEquals(20.0, (double) row.get("deviation"), 0.01); // (60-50)/50 = 20%
    }

    @Test
    void baselineWithCurrent_noBaseline_shouldReturnEmpty() {
        when(baselineMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        BaselineService service = new BaselineService(metricMapper, baselineMapper);
        List<Map<String, Object>> rows = service.baselineWithCurrent(1L);
        assertEquals(0, rows.size());
    }

    @Test
    void baselineWithCurrent_blankBaseline_shouldNullDeviation() {
        MetricBaseline baseline = new MetricBaseline();
        baseline.setInstanceId(1L);
        baseline.setMetricName("CPU");
        baseline.setAvgValue(50.0);
        when(baselineMapper.selectOne(any(Wrapper.class))).thenReturn(baseline, null, null, null, null);
        when(metricMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        BaselineService service = new BaselineService(metricMapper, baselineMapper);
        List<Map<String, Object>> rows = service.baselineWithCurrent(1L);
        assertEquals(1, rows.size());
        assertNull(rows.get(0).get("current"));
        assertNull(rows.get(0).get("deviation"));
    }
}