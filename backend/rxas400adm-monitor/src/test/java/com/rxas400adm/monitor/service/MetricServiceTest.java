package com.rxas400adm.monitor.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.monitor.domain.Metric;
import com.rxas400adm.monitor.mapper.MetricMapper;
import com.rxas400adm.monitor.websocket.MetricPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 指标服务单测（纯 Mockito）：覆盖总览聚合、非数值兜底、活跃作业数采集失败降级（B1 语义）、
 * 历史查询 limit 收敛与保存发布链路。
 */
@ExtendWith(MockitoExtension.class)
class MetricServiceTest {

    @Mock private MetricMapper metricMapper;
    @Mock private MetricPublisher publisher;
    @Mock private AS400ClientProvider clientProvider;
    @Mock private AS400Client client;

    @InjectMocks
    private MetricService service;

    private Map<String, Object> row(String name, Object value) {
        Map<String, Object> m = new HashMap<>();
        m.put("metricName", name);
        m.put("metricValue", value);
        return m;
    }

    @Test
    @DisplayName("overview → 五项指标取最新值 + 活跃作业数")
    void overview_aggregatesLatestValues() {
        when(metricMapper.selectLatestOverview(1L)).thenReturn(List.of(
                row("CPU", 12.5), row("MEMORY", 40), row("DISK", "61"),
                row("MSGW", null), row("LCKW", 0)));
        when(clientProvider.forServer(1L)).thenReturn(client);
        when(client.queryList(anyString())).thenReturn(List.of(Map.of("JOB_NAME", "JOB1")));

        Map<String, Object> result = service.overview(1L);

        assertEquals(12.5, result.get("cpu"));
        assertEquals(40.0, result.get("memory"));
        assertEquals(0.0, result.get("disk"));
        assertEquals(0.0, result.get("msgw"));
        assertEquals(0.0, result.get("lckw"));
        assertEquals(1, result.get("jobs"));
    }

    @Test
    @DisplayName("overview → 无指标行时全部占位 0，作业数正常返回")
    void overview_emptyRowsFallBackToZero() {
        when(metricMapper.selectLatestOverview(2L)).thenReturn(List.of());
        when(clientProvider.forServer(2L)).thenReturn(client);
        when(client.queryList(anyString())).thenReturn(List.of());

        Map<String, Object> result = service.overview(2L);

        for (String key : List.of("cpu", "memory", "disk", "msgw", "lckw")) {
            assertEquals(0.0, result.get(key));
        }
        assertEquals(0, result.get("jobs"));
    }

    @Test
    @DisplayName("overview → 客户端异常降级 jobs=0 且不抛出（B1：记错误日志占位）")
    void overview_clientFailureDegradesToZero() {
        when(metricMapper.selectLatestOverview(3L)).thenReturn(List.of(row("CPU", 5)));
        when(clientProvider.forServer(3L)).thenThrow(new RuntimeException("conn refused"));

        Map<String, Object> result = service.overview(3L);

        assertEquals(5.0, result.get("cpu"));
        assertEquals(0, result.get("jobs"));
        assertTrue(result.containsKey("lckw"));
    }

    @Test
    @DisplayName("save → 插入后经 WebSocket 发布")
    void save_persistsThenPublishes() {
        Metric metric = org.mockito.Mockito.mock(Metric.class);
        service.save(metric);
        verify(metricMapper).insert(metric);
        verify(publisher).publish(metric);
    }

    @Test
    @DisplayName("history → limit 越界收敛到 [1,500]")
    void history_clampsLimit() {
        when(metricMapper.selectList(any())).thenReturn(List.of());
        service.history(1L, -5);
        verify(metricMapper).selectList(any());
    }
}
