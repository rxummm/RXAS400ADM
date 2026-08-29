package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.as400.entity.CommandScript;
import com.rxas400adm.as400.entity.JobSchedule;
import com.rxas400adm.as400.entity.JobScheduleHistory;
import com.rxas400adm.as400.mapper.CommandScriptMapper;
import com.rxas400adm.as400.mapper.JobScheduleHistoryMapper;
import com.rxas400adm.as400.mapper.JobScheduleMapper;
import com.rxas400adm.as400.vo.ExecutionStatsVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, JobScheduleHistory.class);
        TableInfoHelper.initTableInfo(assistant, JobSchedule.class);
        TableInfoHelper.initTableInfo(assistant, CommandScript.class);
    }

    @Mock
    private JobScheduleHistoryMapper historyMapper;
    @Mock
    private JobScheduleMapper scheduleMapper;
    @Mock
    private CommandScriptMapper scriptMapper;

    private ExecutionService service;

    @BeforeEach
    void setUp() {
        service = new ExecutionService(historyMapper, scheduleMapper, scriptMapper);
    }

    private JobScheduleHistory history(Long id, Long scheduleId, String status) {
        JobScheduleHistory h = new JobScheduleHistory();
        h.setId(id);
        h.setScheduleId(scheduleId);
        h.setStatus(status);
        h.setMessage("执行完成");
        h.setRunTime(LocalDateTime.now());
        h.setCostMs(1200L);
        return h;
    }

    private JobSchedule schedule(Long id, String name, String type) {
        JobSchedule s = new JobSchedule();
        s.setId(id);
        s.setName(name);
        s.setScheduleType(type);
        s.setServerId(1L);
        s.setCreatedBy("admin");
        return s;
    }

    @Test
    @DisplayName("scheduleExecutions → 空结果返回空列表")
    void scheduleExecutions_empty_shouldReturnEmpty() {
        when(historyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<Map<String, Object>> result = service.scheduleExecutions(null, null, 10);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("scheduleExecutions → 关联 schedule 信息填充")
    void scheduleExecutions_withSchedule_shouldFillFields() {
        JobScheduleHistory h = history(1L, 10L, "SUCCESS");
        JobSchedule s = schedule(10L, "每日备份", "DAILY");
        when(historyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(h));
        when(scheduleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(s));

        List<Map<String, Object>> result = service.scheduleExecutions(null, null, 10);
        assertEquals(1, result.size());
        assertEquals("每日备份", result.get(0).get("name"));
        assertEquals("DAILY", result.get(0).get("type"));
        assertEquals("SCHEDULE", result.get(0).get("source"));
    }

    @Test
    @DisplayName("scheduleExecutions → status 过滤")
    void scheduleExecutions_withStatus_shouldFilter() {
        when(historyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<Map<String, Object>> result = service.scheduleExecutions("FAILED", null, 10);
        assertNotNull(result);
    }

    @Test
    @DisplayName("scheduleExecutions → keyword 匹配 name/user/message")
    void scheduleExecutions_withKeyword_shouldMatch() {
        JobScheduleHistory h = history(1L, 10L, "SUCCESS");
        h.setMessage("CPU告警触发");
        JobSchedule s = schedule(10L, "告警巡检", "CRON");
        when(historyMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(h));
        when(scheduleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(s));

        List<Map<String, Object>> result = service.scheduleExecutions(null, "告警", 10);
        assertEquals(1, result.size());

        List<Map<String, Object>> noMatch = service.scheduleExecutions(null, "不存在的关键词", 10);
        assertTrue(noMatch.isEmpty());
    }

    @Test
    @DisplayName("scriptExecutions → 脚本执行记录")
    void scriptExecutions_shouldReturn() {
        CommandScript script = new CommandScript();
        script.setId(1L);
        script.setName("DSKINF");
        script.setCreatedBy("admin");
        script.setLastRunTime(LocalDateTime.now());
        script.setLastRunStatus("SUCCESS");
        script.setLastResult("正常");
        when(scriptMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(script));

        List<Map<String, Object>> result = service.scriptExecutions(null, null, 10);
        assertEquals(1, result.size());
        assertEquals("SCRIPT", result.get(0).get("source"));
        assertEquals("CL", result.get(0).get("type"));
    }

    @Test
    @DisplayName("scriptExecutions → null lastRunStatus 按 FAILED 处理")
    void scriptExecutions_nullStatus_shouldTreatAsFailed() {
        CommandScript script = new CommandScript();
        script.setId(1L);
        script.setName("TEST");
        script.setLastRunTime(LocalDateTime.now());
        script.setLastRunStatus(null); // null status → FAILED
        when(scriptMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(script));

        List<Map<String, Object>> failed = service.scriptExecutions("FAILED", null, 10);
        assertEquals(1, failed.size());

        List<Map<String, Object>> success = service.scriptExecutions("SUCCESS", null, 10);
        assertTrue(success.isEmpty());
    }

    @Test
    @DisplayName("getStats → 空表返回零值")
    void getStats_empty_shouldReturnZeros() {
        when(historyMapper.selectExecutionStats()).thenReturn(null);

        ExecutionStatsVO stats = service.getStats();
        assertEquals(0, stats.totalExecutions());
        assertEquals(0, stats.successCount());
        assertEquals(0, stats.failedCount());
        assertEquals(0, stats.successRate());
    }

    @Test
    @DisplayName("getStats → 正常计算成功率")
    void getStats_withData_shouldCalculateRate() {
        Map<String, Object> row = Map.of(
                "totalCnt", 100,
                "successCnt", 95,
                "avgCost", 500.0
        );
        when(historyMapper.selectExecutionStats()).thenReturn(row);

        ExecutionStatsVO stats = service.getStats();
        assertEquals(100, stats.totalExecutions());
        assertEquals(95, stats.successCount());
        assertEquals(5, stats.failedCount());
        assertEquals(95.0, stats.successRate(), 0.01);
        assertEquals(500.0, stats.avgCostMs());
    }
}
