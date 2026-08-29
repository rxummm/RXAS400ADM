package com.rxas400adm.config;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.config.vo.HealthReportVO;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 【R2】编排下沉后：Controller 仅委托 HealthService，
 * 本测试改为构造真实 HealthService（协作者全 mock、探测用直通执行器保持同步）验证聚合语义。
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private IbmiSystemMapper systemMapper;

    @Mock
    private AlertEventMapper alertEventMapper;

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    @Mock
    private Scheduler scheduler;

    private HealthController controllerWithStubs() {
        // 直通执行器：并行探测在测试线程内同步执行
        HealthService healthService = new HealthService(
                systemMapper, alertEventMapper, clientProvider, scheduler, Runnable::run);
        return new HealthController(healthService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void report_shouldAggregateAllChecks() throws Exception {
        IbmiSystem system = new IbmiSystem();
        system.setId(1L);
        system.setName("US400CND");
        system.setHost("10.0.0.1");
        system.setEnvironment("PROD");
        system.setSortOrder(1);

        when(systemMapper.selectCount(any())).thenReturn(1L);
        when(systemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(system));
        when(alertEventMapper.selectCount(any(Wrapper.class))).thenReturn(2L);
        when(clientProvider.forServer(1L)).thenReturn(client);
        when(client.testConnection()).thenReturn(CommandResult.ok("OK"));
        when(scheduler.isStarted()).thenReturn(true);

        ApiResponse<HealthReportVO> response = controllerWithStubs().report();

        HealthReportVO report = response.getData();
        assertEquals("OK", report.database());
        assertEquals("RUNNING", report.scheduler());
        assertEquals(2L, report.openAlerts());
        assertEquals(1, report.servers().size());
        assertEquals("OK", report.servers().get(0).connect());
        assertEquals("US400CND", report.servers().get(0).name());
    }

    @Test
    @SuppressWarnings("unchecked")
    void report_connectionFailure_shouldMarkFail() throws Exception {
        IbmiSystem system = new IbmiSystem();
        system.setId(2L);
        system.setName("TEST01");
        system.setHost("10.0.0.2");
        system.setEnvironment("TEST");
        system.setSortOrder(1);

        when(systemMapper.selectCount(any())).thenReturn(1L);
        when(systemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(system));
        when(alertEventMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(clientProvider.forServer(2L)).thenReturn(client);
        when(client.testConnection()).thenReturn(CommandResult.fail("timeout"));
        when(scheduler.isStarted()).thenReturn(false);

        ApiResponse<HealthReportVO> response = controllerWithStubs().report();

        HealthReportVO report = response.getData();
        assertEquals("STOPPED", report.scheduler());
        assertEquals("FAIL", report.servers().get(0).connect());
    }
}