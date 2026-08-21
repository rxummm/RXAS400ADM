package com.rxas400adm.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P3：ReportScheduleQuartzJob 由静态持有者改为 Spring Bean（@Autowired 字段注入）后的行为单测。
 */
@ExtendWith(MockitoExtension.class)
class ReportScheduleQuartzJobTest {

    @Mock
    private ReportScheduleService reportScheduleService;

    @Mock
    private JobExecutionContext context;

    private ReportScheduleQuartzJob newJob() {
        ReportScheduleQuartzJob job = new ReportScheduleQuartzJob();
        ReflectionTestUtils.setField(job, "reportScheduleService", reportScheduleService);
        return job;
    }

    private void stubScheduleId(long id) {
        JobDataMap data = new JobDataMap();
        data.put("scheduleId", id);
        when(context.getMergedJobDataMap()).thenReturn(data);
    }

    @Test
    void execute_shouldDelegateScheduleIdToService() throws Exception {
        stubScheduleId(42L);
        newJob().execute(context);
        verify(reportScheduleService).execute(42L);
    }

    @Test
    void execute_serviceNotInjected_shouldSkipWithoutError() throws Exception {
        stubScheduleId(1L);
        ReportScheduleQuartzJob job = new ReportScheduleQuartzJob(); // 不注入 → 模拟服务未就绪
        job.execute(context);
        verify(reportScheduleService, never()).execute(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void execute_serviceThrows_shouldNotPropagate() throws Exception {
        stubScheduleId(7L);
        doThrow(new RuntimeException("boom")).when(reportScheduleService).execute(7L);
        assertDoesNotThrow(() -> newJob().execute(context));
        verify(reportScheduleService).execute(7L);
    }
}
