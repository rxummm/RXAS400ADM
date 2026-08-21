package com.rxas400adm.monitor.collector;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.monitor.domain.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiskCollectorTest {

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    private DiskCollector collector;

    @BeforeEach
    void setUp() {
        collector = new DiskCollector(clientProvider);
        when(clientProvider.forServer(1L)).thenReturn(client);
    }

    @Test
    void collect_shouldComputeSysbasUsagePercent() {
        when(client.queryList(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(
                Map.of("ASP_NAME", "SYSBAS", "TOTAL_SPACE", 1000.0, "USED_SPACE", 780.0),
                Map.of("ASP_NAME", "ASP01", "TOTAL_SPACE", 500.0, "USED_SPACE", 250.0)
        ));
        Metric metric = collector.collect(1L);
        assertEquals("DISK", metric.getMetricName());
        assertEquals(78.0, metric.getMetricValue(), 0.01);
    }

    @Test
    void collect_noData_shouldReturnZero() {
        when(client.queryList(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        Metric metric = collector.collect(1L);
        assertEquals(0.0, metric.getMetricValue(), 0.01);
    }

    @Test
    void collect_badRows_shouldNotThrow() {
        when(client.queryList(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(
                Map.of("ASP_NAME", "SYSBAS", "TOTAL_SPACE", "X", "USED_SPACE", "Y")
        ));
        Metric metric = collector.collect(1L);
        assertTrue(metric.getMetricValue() >= 0);
    }
}
