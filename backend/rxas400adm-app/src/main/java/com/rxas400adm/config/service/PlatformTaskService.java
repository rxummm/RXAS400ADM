package com.rxas400adm.config.service;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import com.rxas400adm.config.vo.TaskInfoVO;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
/**
 * 平台定时任务服务：@Scheduled 任务自省列举 + 手动触发。
 *
 * 从 PlatformTaskController 下沉的五阶段业务编排：
 * ① 白名单校验 → ② 每任务限频 → ③ Bean 解析 → ④ 反射合法性校验 → ⑤ 异步执行。
 * Controller 只保留 HTTP 语义与权限注解，本类可独立单测。
 */
@Slf4j
@Service
public class PlatformTaskService {

    private final ApplicationContext applicationContext;
    private final Executor platformTaskPool;

    /** P2-16：仅暴露本平台自身（com.rxas400adm）的 @Scheduled 任务，第三方/框架 Bean 一律不可手动触发 */
    private static final String ALLOWED_PACKAGE = "com.rxas400adm";

    /**
     * M4：任务名白名单（beanName.methodName，逗号分隔，可配置）。
     * 为空时回退到「本平台包内已注册 @Scheduled 任务」这一最小白名单；
     * 配置后仅白名单内的任务可被手动触发（配置即收紧）。
     */
    @Value("${rxas400.task.trigger-whitelist:}")
    private String triggerWhitelist;

    /** M4：每任务手动触发限频（毫秒），防止洪峰反复触发破坏性维护 */
    private static final long TRIGGER_MIN_INTERVAL_MS = 5_000;
    /** C13：CAS 时间槽——原 putIfAbsent→判断→put 三段式非原子；merge 方案在同毫秒重复触发时会误放行 */
    private final ConcurrentHashMap<String, AtomicLong> lastTriggeredAt = new ConcurrentHashMap<>();

    /** C5：执行中任务集合——同一任务手动触发未结束前拒绝再次触发（含与调度 tick 撞车时的手动侧防护） */
    private final Set<String> runningTriggers = ConcurrentHashMap.newKeySet();

    // Spring 容器管理线程池生命周期，无需手动 shutdown

    public PlatformTaskService(ApplicationContext applicationContext, Executor platformTaskPool) {
        this.applicationContext = applicationContext;
        this.platformTaskPool = platformTaskPool;
    }

    /** 列举容器内本平台包的 @Scheduled 任务及触发配置 */
    public List<TaskInfoVO> listTasks() {
        List<TaskInfoVO> result = new ArrayList<>();
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e) {
                continue;
            }
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            // P2-16：收紧到本平台包，不展示框架/第三方 @Scheduled Bean
            if (!targetClass.getName().startsWith(ALLOWED_PACKAGE)) {
                continue;
            }
            List<TaskInfoVO.ScheduledMethodVO> methods = new ArrayList<>();
            for (Method method : Arrays.stream(targetClass.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Scheduled.class)).toList()) {
                Scheduled scheduled = method.getAnnotation(Scheduled.class);
                String schedule = buildScheduleDesc(scheduled);
                boolean enabled = CronExpression.isValidExpression(scheduled.cron())
                        || scheduled.fixedDelay() >= 0 || scheduled.fixedRate() >= 0;
                methods.add(new TaskInfoVO.ScheduledMethodVO(method.getName(), schedule, enabled));
            }
            if (!methods.isEmpty()) {
                result.add(new TaskInfoVO(beanName, targetClass.getSimpleName(), methods));
            }
        }
        return result;
    }

    /**
     * 手动触发一次（异步执行）：白名单 → 限频 → Bean 解析 → 反射校验 → 提交执行。
     * 校验失败抛 BusinessException；通过后立即返回。
     */
    public LocalDateTime trigger(String beanName, String methodName) {
        checkWhitelist(beanName, methodName);
        checkRateLimit(beanName, methodName);
        Object bean = resolveBean(beanName);
        Method method = validateMethod(bean, beanName, methodName);
        String taskKey = beanName + "." + methodName;
        // C5：同任务上一轮手动触发尚未结束时拒绝重入
        if (!runningTriggers.add(taskKey)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Task is currently running, please retry later: " + taskKey);
        }
        platformTaskPool.execute(() -> {
            try {
                log.info("[任务] 手动触发 {} 开始", taskKey);
                method.invoke(bean);
                log.info("[任务] 手动触发 {} 完成", taskKey);
            } catch (Exception e) {
                log.error("[任务] 手动触发 {} 失败: {}", taskKey, e.getMessage(), e);
            } finally {
                runningTriggers.remove(taskKey);
            }
        });
        return LocalDateTime.now();
    }

    // ---------------- 五阶段私有实现 ----------------

    /** 阶段①：M4 任务名白名单——配置了 trigger-whitelist 时仅白名单内任务可触发（仍须经过包/注册校验） */
    private void checkWhitelist(String beanName, String methodName) {
        Set<String> whitelist = parseWhitelist();
        if (!whitelist.isEmpty() && !whitelist.contains(beanName + "." + methodName)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Task not in trigger whitelist: " + beanName + "." + methodName);
        }
    }

    /**
     * 阶段②：M4 每任务限频——同任务 5 秒内不得重复手动触发。
     * C13：AtomicLong CAS 实现，原子且正确处理「同毫秒第二次触发」。
     */
    private void checkRateLimit(String beanName, String methodName) {
        long now = System.currentTimeMillis();
        AtomicLong slot = lastTriggeredAt.computeIfAbsent(beanName + "." + methodName, k -> new AtomicLong());
        while (true) {
            long prev = slot.get();
            long wait = TRIGGER_MIN_INTERVAL_MS - (now - prev);
            if (prev != 0L && wait > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Trigger too frequent, please retry in " + (wait / 1000 + 1) + " seconds");
            }
            if (slot.compareAndSet(prev, now)) {
                return;
            }
        }
    }

    /** 阶段③：Bean 解析 */
    private Object resolveBean(String beanName) {
        try {
            return applicationContext.getBean(beanName);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Task bean not found: " + beanName);
        }
    }

    /** 阶段④：P2-16 只允许触发本平台包内、无参、非私有（@Scheduled 方法本就 public）的定时方法 */
    private Method validateMethod(Object bean, String beanName, String methodName) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        if (!targetClass.getName().startsWith(ALLOWED_PACKAGE)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot trigger external bean tasks: " + beanName);
        }
        Method method = Arrays.stream(targetClass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Scheduled.class) && m.getName().equals(methodName))
                .findFirst().orElse(null);
        if (method == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "Method not found or not a scheduled task: " + beanName + "." + methodName);
        }
        if (method.getParameterCount() != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Only parameterless scheduled methods can be triggered: " + beanName + "." + methodName);
        }
        return method;
    }

    /** M4：解析任务白名单属性（逗号分隔 beanName.methodName），去空白去重。 */
    Set<String> parseWhitelist() {
        if (triggerWhitelist == null || triggerWhitelist.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(triggerWhitelist.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private String buildScheduleDesc(Scheduled scheduled) {
        if (scheduled.cron() != null && !scheduled.cron().isBlank()) {
            return "cron: " + scheduled.cron();
        }
        if (scheduled.fixedDelay() >= 0) {
            return "fixedDelay: " + scheduled.fixedDelay() + "ms";
        }
        if (scheduled.fixedRate() >= 0) {
            return "fixedRate: " + scheduled.fixedRate() + "ms";
        }
        return "not configured";
    }
}
