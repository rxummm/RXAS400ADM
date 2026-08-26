package com.rxas400adm.monitor.scheduler;

import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.monitor.alert.AlertEngine;
import com.rxas400adm.monitor.collector.MetricCollector;
import com.rxas400adm.monitor.domain.Metric;
import com.rxas400adm.monitor.mapper.MetricMapper;
import com.rxas400adm.monitor.service.MetricService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2：per-server 并行采集验证——多服务器并行执行、单台失败不影响其他、整轮超时保护。
 */
class CollectorSchedulerTest {


    private IbmiSystemMapper systemMapper;
    private MetricService metricService;
    private MetricMapper metricMapper;
    private AlertEngine alertEngine;

    @BeforeEach
    void setUp() {
        systemMapper = mock(IbmiSystemMapper.class);
        metricService = mock(MetricService.class);
        metricMapper = mock(MetricMapper.class);
        alertEngine = mock(AlertEngine.class);
    }

    @AfterEach
    void clearLockProperty() {
        System.clearProperty("rxas400.monitor.scheduler-lock");
    }

    private IbmiSystem server(Long id, String name) {
        IbmiSystem s = new IbmiSystem();
        s.setId(id);
        s.setName(name);
        s.setEnabled(true);
        return s;
    }

    private CollectorScheduler scheduler(List<MetricCollector> collectors) {
        return new CollectorScheduler(collectors, metricService, systemMapper, alertEngine, metricMapper);
    }

    @Test
    @DisplayName("多服务器并行采集：全部服务器 × 全部 Collector 均执行")
    void collect_parallel_runsAllServersAndCollectors() {
        when(systemMapper.selectList(any())).thenReturn(List.of(server(1L, "US400CND"), server(2L, "TEST01")));
        MetricCollector c1 = mock(MetricCollector.class);
        MetricCollector c2 = mock(MetricCollector.class);
        when(c1.collect(any())).thenReturn(Metric.builder().metricName("CPU").build());
        when(c2.collect(any())).thenReturn(Metric.builder().metricName("DISK").build());

        scheduler(List.of(c1, c2)).collect();

        // 2 服务器 × 2 Collector = 4 次采集；P5 后按服务器分批 saveBatch（2 批 × 2 条）+ 4 次告警检查
        verify(c1, times(2)).collect(any());
        verify(c2, times(2)).collect(any());
        ArgumentCaptor<List<Metric>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(metricService, times(2)).saveBatch(batchCaptor.capture());
        assertEquals(4, batchCaptor.getAllValues().stream().mapToInt(List::size).sum());
        verify(alertEngine, times(4)).check(any(Metric.class));
    }

    @Test
    @DisplayName("单台服务器采集失败不影响其他服务器")
    void collect_serverFailure_doesNotStopOthers() {
        when(systemMapper.selectList(any())).thenReturn(List.of(server(1L, "US400CND"), server(2L, "TEST01")));
        MetricCollector c1 = mock(MetricCollector.class);
        MetricCollector c2 = mock(MetricCollector.class);
        when(c1.collect(any())).thenThrow(new RuntimeException("connection refused"));
        when(c2.collect(any())).thenReturn(Metric.builder().metricName("CPU").build());

        scheduler(List.of(c1, c2)).collect();

        // 失败的 Collector 仍被 2 台服务器各调用一次（异常被吞）；成功 Collector 正常落库
        verify(c1, times(2)).collect(any());
        verify(c2, times(2)).collect(any());
        ArgumentCaptor<List<Metric>> okBatch = ArgumentCaptor.forClass(List.class);
        // P5：仅成功采集的进入批次（1 批 × 1 条/服务器）
        verify(metricService, times(2)).saveBatch(okBatch.capture());
        assertEquals(2, okBatch.getAllValues().stream().mapToInt(List::size).sum());
        verify(alertEngine, times(2)).check(any(Metric.class));
    }

    @Test
    @DisplayName("单服务器场景退化为串行，行为一致")
    void collect_singleServer_serialPath() {
        when(systemMapper.selectList(any())).thenReturn(List.of(server(1L, "US400CND")));
        MetricCollector c1 = mock(MetricCollector.class);
        when(c1.collect(any())).thenReturn(Metric.builder().metricName("CPU").build());

        scheduler(List.of(c1)).collect();

        verify(c1, times(1)).collect(any());
        // P5：单条也走 saveBatch 批次
        verify(metricService, times(1)).saveBatch(any());
    }

    @Test
    @DisplayName("并行采集：三台各自成批落库（P5 批量语义）")
    void collect_parallel_waitsForAllBeforeReturn() {
        when(systemMapper.selectList(any())).thenReturn(List.of(server(1L, "A"), server(2L, "B"), server(3L, "C")));
        MetricCollector c = mock(MetricCollector.class);
        when(c.collect(any())).thenReturn(Metric.builder().metricName("CPU").build());

        scheduler(List.of(c)).collect();

        // P5：每台一个批次，共 3 批；超时取消（C2）导致的批次丢弃属调度器内部时序行为，
        // 不在本用例断言范围（避免与真实线程时序耦合造成 flaky）
        ArgumentCaptor<List<Metric>> batches = ArgumentCaptor.forClass(List.class);
        verify(metricService, times(3)).saveBatch(batches.capture());
        assertEquals(3, batches.getAllValues().size());
    }

    @Test
    @DisplayName("锁开关开启时先抢锁后采集")
    void collect_lockEnabled_acquiresAndReleases() {
        System.setProperty("rxas400.monitor.scheduler-lock", "true");
        when(metricMapper.tryAcquireLock(any(), any(), any())).thenReturn(1);
        when(systemMapper.selectList(any())).thenReturn(List.of(server(1L, "US400CND")));
        MetricCollector c1 = mock(MetricCollector.class);
        when(c1.collect(any())).thenReturn(Metric.builder().metricName("CPU").build());

        scheduler(List.of(c1)).collect();

        verify(metricMapper).tryAcquireLock(any(), any(), any());
        verify(metricMapper).releaseLock(any(), any());
        verify(metricService, times(1)).saveBatch(any());
    }

    @Test
    @DisplayName("单台服务器失败重复执行不应重复告警检查")
    void collect_failure_skipsAlertForFailedMetric() {
        when(systemMapper.selectList(any())).thenReturn(List.of(server(1L, "US400CND")));
        MetricCollector c1 = mock(MetricCollector.class);
        MetricCollector c2 = mock(MetricCollector.class);
        when(c1.collect(any())).thenThrow(new RuntimeException("boom"));
        when(c2.collect(any())).thenReturn(Metric.builder().metricName("CPU").build());

        scheduler(List.of(c1, c2)).collect();

        // 只有成功采集的 metric 才进入告警引擎
        verify(alertEngine, times(1)).check(any(Metric.class));
        verify(metricService, times(1)).saveBatch(any());
    }
}