package com.rxas400adm.report;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 报表定时 Quartz 执行体：从 JobDataMap 取 scheduleId 委托给 ReportScheduleService.execute。
 *
 * Spring Boot 3.3 的 Quartz 自动配置使用 SpringBeanJobFactory——它对反射创建的作业实例
 * 执行 autowireBean（支持字段/Setter 注入，不支持构造器注入），因此这里用 @Autowired
 * 字段注入替代构造器注入，消除静态可变持有者。服务未就绪时仍防御性跳过。
 */
@Slf4j
@Component
@SuppressWarnings("java:S6813")
public class ReportScheduleQuartzJob implements Job {

    @Autowired
    private IReportScheduleService reportScheduleService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long scheduleId = context.getMergedJobDataMap().getLong("scheduleId");
        IReportScheduleService service = reportScheduleService;
        if (service == null) {
            log.error("[报表定时] 服务未初始化，跳过任务 {}", scheduleId);
            return;
        }
        try {
            service.execute(scheduleId);
        } catch (Exception e) {
            log.error("[报表定时] Quartz 任务 {} 执行异常: {}", scheduleId, e.getMessage());
        }
    }
}