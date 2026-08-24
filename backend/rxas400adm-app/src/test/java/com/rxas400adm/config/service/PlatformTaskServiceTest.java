package com.rxas400adm.config.service;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.config.vo.TaskInfoVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * PlatformTaskService 单测：覆盖手动触发五阶段编排
 * （白名单 → 限频 → Bean 解析 → 反射校验 → 异步执行）与任务列举过滤。
 * Executor 用同步直接执行器，便于断言副作用。
 */
@ExtendWith(MockitoExtension.class)
class PlatformTaskServiceTest {

    @Mock
    private ApplicationContext applicationContext;

    private PlatformTaskService service;

    /** 同步执行器：trigger 返回时任务已跑完，方便断言 */
    private final Executor directExecutor = Runnable::run;

    /** 测试夹具：本平台包内带 @Scheduled 方法的 Bean */
    static class DemoTask {
        final AtomicInteger runs = new AtomicInteger();

        @Scheduled(fixedDelay = 60_000)
        public void sweep() {
            runs.incrementAndGet();
        }

        @Scheduled(fixedDelay = 60_000)
        public void withArgs(int x) {
            // 仅用于「有参定时方法」校验路径
        }
    }

    @BeforeEach
    void setUp() {
        service = new PlatformTaskService(applicationContext, directExecutor);
        ReflectionTestUtils.setField(service, "triggerWhitelist", "");
    }

    private void givenBean(String beanName, Object bean) {
        when(applicationContext.getBean(beanName)).thenReturn(bean);
    }

    @Test
    void listTasks_filtersThirdPartyBeans() {
        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[]{"demoTask", "thirdParty"});
        when(applicationContext.getBean("demoTask")).thenReturn(new DemoTask());
        when(applicationContext.getBean("thirdParty")).thenReturn("java.lang.String 不是平台包 Bean");

        List<TaskInfoVO> result = service.listTasks();
        assertEquals(1, result.size());
        assertEquals("DemoTask", result.get(0).className());
        assertTrue(result.get(0).methods().stream().anyMatch(m -> "sweep".equals(m.method())));
    }

    @Test
    void trigger_whitelistRejectsForeignTask() {
        ReflectionTestUtils.setField(service, "triggerWhitelist", "otherBean.run");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.trigger("demoTask", "sweep"));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void trigger_secondCallWithinIntervalIsRateLimited() {
        givenBean("demoTask", new DemoTask());
        service.trigger("demoTask", "sweep");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.trigger("demoTask", "sweep"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void trigger_beanNotFound() {
        when(applicationContext.getBean("ghost")).thenThrow(new RuntimeException("no such bean"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.trigger("ghost", "run"));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void trigger_externalPackageBeanRejected() {
        Object foreign = new Object();
        when(applicationContext.getBean("foreign")).thenReturn(foreign);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.trigger("foreign", "wait"));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void trigger_methodNotScheduledRejected() {
        givenBean("demoTask", new DemoTask());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.trigger("demoTask", "notScheduledMethod"));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void trigger_methodWithParametersRejected() {
        DemoTask demo = new DemoTask();
        givenBean("demoTask", demo);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.trigger("demoTask", "withArgs"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals(0, demo.runs.get());
    }

    @Test
    void trigger_successInvokesAndReturnsTimestamp() {
        DemoTask demo = new DemoTask();
        givenBean("demoTask", demo);
        service.trigger("demoTask", "sweep");
        assertEquals(1, demo.runs.get());
        // 5 秒限频窗口内的第二次触发应被拒绝，且任务未再次执行
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.trigger("demoTask", "sweep"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals(1, demo.runs.get());
    }
}
