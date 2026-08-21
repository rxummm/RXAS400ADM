package com.rxas400adm.as400.service;

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
 * P3：ScheduleQuartzJob 由静态持有者改为 Spring Bean（@Autowired 字段注入）后的行为单测。
 * 反射注入 mock 等价于 SpringBeanJobFactory.autowireBean 的字段注入路径。
 */
@ExtendWith(MockitoExtension.class)
class ScheduleQuartzJobTest {

    @Mock
    private JobScheduleService jobScheduleService;

    @Mock
    private JobExecutionContext context;

    private ScheduleQuartzJob newJob() {
        ScheduleQuartzJob job = new ScheduleQuartzJob();
        ReflectionTestUtils.setField(job, "jobScheduleService", jobScheduleService);
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
        verify(jobScheduleService).execute(42L);
    }

    @Test
    void execute_serviceNotInjected_shouldSkipWithoutError() throws Exception {
        stubScheduleId(1L);
        ScheduleQuartzJob job = new ScheduleQuartzJob(); // 不注入 → 模拟服务未就绪
        job.execute(context);
        verify(jobScheduleService, never()).execute(anyScheduleId());
    }

    @Test
    void execute_serviceThrows_shouldNotPropagate() throws Exception {
        stubScheduleId(7L);
        doThrow(new RuntimeException("boom")).when(jobScheduleService).execute(7L);
        assertDoesNotThrow(() -> newJob().execute(context));
        verify(jobScheduleService).execute(7L);
    }

    private static long anyScheduleId() {
        return org.mockito.ArgumentMatchers.anyLong();
    }
}
