package com.rxas400adm.config;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.config.vo.HealthReportVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private HealthService healthService;

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    @Mock
    private Scheduler scheduler;

    @Test
    void report_shouldAggregateAllChecks() throws Exception {
        IbmiSystem system = new IbmiSystem();
        system.setId(1L);
        system.setName("US400CND");
        system.setHost("10.0.0.1");
        system.setEnvironment("PROD");

        when(healthService.probeDatabase()).thenReturn(true);
        when(healthService.listSystemsOrdered()).thenReturn(List.of(system));
        when(healthService.countOpenAlerts()).thenReturn(2L);
        when(clientProvider.forServer(1L)).thenReturn(client);
        when(client.testConnection()).thenReturn(CommandResult.ok("OK"));
        when(scheduler.isStarted()).thenReturn(true);

        HealthController controller = new HealthController(healthService, clientProvider, scheduler);
        ApiResponse<HealthReportVO> response = controller.report();

        HealthReportVO report = response.getData();
        assertEquals("OK", report.database());
        assertEquals("RUNNING", report.scheduler());
        assertEquals(2L, report.openAlerts());
        assertEquals(1, report.servers().size());
        assertEquals("OK", report.servers().get(0).connect());
        assertEquals("US400CND", report.servers().get(0).name());
    }

    @Test
    void report_connectionFailure_shouldMarkFail() throws Exception {
        IbmiSystem system = new IbmiSystem();
        system.setId(2L);
        system.setName("TEST01");
        system.setHost("10.0.0.2");
        system.setEnvironment("TEST");

        when(healthService.probeDatabase()).thenReturn(true);
        when(healthService.listSystemsOrdered()).thenReturn(List.of(system));
        when(healthService.countOpenAlerts()).thenReturn(0L);
        when(clientProvider.forServer(2L)).thenReturn(client);
        when(client.testConnection()).thenReturn(CommandResult.fail("timeout"));
        when(scheduler.isStarted()).thenReturn(false);

        HealthController controller = new HealthController(healthService, clientProvider, scheduler);
        ApiResponse<HealthReportVO> response = controller.report();

        HealthReportVO report = response.getData();
        assertEquals("STOPPED", report.scheduler());
        assertEquals("FAIL", report.servers().get(0).connect());
    }
}
