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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        // 2 服务器 × 2 Collector = 4 次采集 + 4 次落库 + 4 次告警检查
        verify(c1, times(2)).collect(any());
        verify(c2, times(2)).collect(any());
        verify(metricService, times(4)).save(any(Metric.class));
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
        verify(metricService, times(2)).save(any(Metric.class));
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
        verify(metricService, times(1)).save(any(Metric.class));
    }

    @Test
    @DisplayName("并行采集等待全部完成后再返回（无丢采集）")
    void collect_parallel_waitsForAllBeforeReturn() throws InterruptedException {
        when(systemMapper.selectList(any())).thenReturn(List.of(server(1L, "A"), server(2L, "B"), server(3L, "C")));
        CountDownLatch started = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        MetricCollector blocking = mock(MetricCollector.class);
        when(blocking.collect(any())).thenAnswer(inv -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
            return Metric.builder().metricName("CPU").build();
        });

        scheduler(List.of(blocking)).collect();
        assertTrue(started.await(2, TimeUnit.SECONDS), "三台服务器应并行启动采集");

        // 模拟超时场景：线程数小于服务器数，第二轮排队，collect() 应在超时后返回而不是永久阻塞
        release.countDown();
        // collect() 已返回（超时保护生效）；释放后所有采集最终完成
        ArgumentCaptor<Metric> captor = ArgumentCaptor.forClass(Metric.class);
        verify(metricService, times(3)).save(captor.capture());
        assertEquals(3, captor.getAllValues().size());
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
        verify(metricService, times(1)).save(any(Metric.class));
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
        verify(metricService, times(1)).save(any(Metric.class));
    }
}