package com.rxas400adm.report;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.rxas400adm.as400.entity.CommandScript;
import com.rxas400adm.as400.entity.JobSchedule;
import com.rxas400adm.as400.entity.JobScheduleHistory;
import com.rxas400adm.as400.mapper.CommandScriptMapper;
import com.rxas400adm.as400.mapper.JobScheduleHistoryMapper;
import com.rxas400adm.as400.mapper.JobScheduleMapper;
import com.rxas400adm.monitor.mapper.MetricMapper;
import com.rxas400adm.monitor.service.CapacityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ReportServiceTest {

    @Mock
    private MetricMapper metricMapper;

    @Mock
    private JobScheduleHistoryMapper historyMapper;

    @Mock
    private JobScheduleMapper scheduleMapper;

    @Mock
    private CommandScriptMapper scriptMapper;

    @Mock
    private CapacityService capacityService;

    private ReportService service() {
        return new ReportService(metricMapper, historyMapper, scheduleMapper, scriptMapper, capacityService);
    }

    /** 数据库侧 GROUP BY 聚合结果（与 MetricMapper.selectAggregatedMetrics 行结构一致） */
    private List<Map<String, Object>> aggregatedRows() {
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("d", "2026-08-10");
        cpu.put("m", "CPU");
        cpu.put("avg", 52.5);
        cpu.put("max", 55.0);
        cpu.put("min", 50.0);
        cpu.put("samples", 6);
        Map<String, Object> mem = new LinkedHashMap<>();
        mem.put("d", "2026-08-11");
        mem.put("m", "MEM");
        mem.put("avg", 61.3);
        mem.put("max", 72.1);
        mem.put("min", 55.0);
        mem.put("samples", 5);
        return List.of(cpu, mem);
    }

    @Test
    void metricsRows_shouldAggregateByDateAndMetric() {
        when(metricMapper.selectAggregatedMetrics(eq(1L), any(LocalDateTime.class))).thenReturn(aggregatedRows());
        List<Map<String, Object>> rows = service().metricsRows(1L, 7);
        // 数据库聚合行原样透传映射为报表行结构（d→date、m→metric），无 Java 侧二次聚合
        assertEquals(2, rows.size());
        Map<String, Object> first = rows.get(0);
        assertEquals("2026-08-10", first.get("date"));
        assertEquals("CPU", first.get("metric"));
        assertEquals(52.5, ((Number) first.get("avg")).doubleValue());
        assertEquals(55.0, ((Number) first.get("max")).doubleValue());
        assertEquals(50.0, ((Number) first.get("min")).doubleValue());
        assertEquals(6, ((Number) first.get("samples")).intValue());
        // GROUP BY 下推：不再走全量 selectList，且窗口起算时间正确传入（now - days）
        verify(metricMapper).selectAggregatedMetrics(eq(1L), any(LocalDateTime.class));
        verify(metricMapper, org.mockito.Mockito.never()).selectList(any(Wrapper.class));
    }

    @Test
    void executionRows_shouldMergeScheduleAndScript() {
        JobScheduleHistory history = new JobScheduleHistory();
        history.setId(1L);
        history.setScheduleId(10L);
        history.setRunTime(LocalDateTime.now());
        history.setStatus("SUCCESS");
        history.setMessage("ok");
        history.setCostMs(120L);
        when(historyMapper.selectList(any(Wrapper.class))).thenReturn(List.of(history));

        JobSchedule schedule = new JobSchedule();
        schedule.setId(10L);
        schedule.setName("备份任务");
        schedule.setScheduleType("CL");
        schedule.setServerId(1L);
        schedule.setCreatedBy("admin");
        when(scheduleMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(schedule));

        CommandScript script = new CommandScript();
        script.setId(2L);
        script.setName("收集作业");
        script.setCreatedBy("admin");
        script.setLastRunTime(LocalDateTime.now());
        script.setLastRunStatus("SUCCESS");
        when(scriptMapper.selectList(any(Wrapper.class))).thenReturn(List.of(script));

        List<Map<String, Object>> rows = service().executionRows(null, null);
        assertEquals(2, rows.size());
        Map<String, Object> schedRow = rows.stream().filter(r -> "SCHEDULE".equals(r.get("source"))).findFirst().orElseThrow();
        assertEquals("备份任务", schedRow.get("name"));
        Map<String, Object> scriptRow = rows.stream().filter(r -> "SCRIPT".equals(r.get("source"))).findFirst().orElseThrow();
        assertEquals("SUCCESS", scriptRow.get("status"));
    }

    @Test
    void renderExcel_shouldProduceXlsxMagic() {
        byte[] data = service().render("xlsx", "测试报表",
                new String[]{"date", "metric", "avg"},
                List.of(Map.of("date", "2026-08-12", "metric", "CPU", "avg", 58.5)));
        assertTrue(data.length > 0);
        assertArrayEquals(new byte[]{'P', 'K'}, new byte[]{data[0], data[1]}); // xlsx = zip magic
    }

    @Test
    void renderPdf_shouldProducePdfMagic() {
        byte[] data = service().render("pdf", "Test Report",
                new String[]{"date", "metric", "avg"},
                List.of(Map.of("date", "2026-08-12", "metric", "CPU", "avg", 58.5)));
        assertTrue(data.length > 0);
        assertEquals('%', (char) data[0]); // %PDF
    }
}