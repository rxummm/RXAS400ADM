package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.dto.JobScheduleRequest;
import com.rxas400adm.as400.entity.JobSchedule;
import com.rxas400adm.as400.entity.JobScheduleHistory;
import com.rxas400adm.as400.entity.ScheduleAlertEvent;
import com.rxas400adm.as400.mapper.JobScheduleHistoryMapper;
import com.rxas400adm.as400.mapper.JobScheduleMapper;
import com.rxas400adm.as400.mapper.ScheduleAlertEventMapper;
import com.rxas400adm.as400.util.CronValidator;
import com.rxas400adm.as400.vo.ScheduleExecuteResultVO;
import com.rxas400adm.common.constants.ExecutionStatus;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.common.event.AlertRaisedEvent;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.security.DangerousClCommandValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 作业调度中心（追踪文档 2.4.4，参照旧项目 As400JobScheduleController）：\n * 定时执行 CL 命令 / SQL。任务定义持久化在 rx_job_schedule，\n * Quartz（spring-boot-starter-quartz，RAMJobStore）按 cron 触发；\n * 启动时自动把启用中的任务重新注册到调度器（重启不丢）。\n */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobScheduleService implements IJobScheduleService, ApplicationRunner {

    private final JobScheduleMapper scheduleMapper;
    private final JobScheduleHistoryMapper historyMapper;
    private final AS400ClientProvider clientProvider;
    private final Scheduler scheduler;
    private final ScheduleAlertEventMapper alertEventMapper;
    private final ApplicationEventPublisher eventPublisher;
    /** S4：高危 CL 动词黑名单校验（execute 的 CL 分支兜底） */
    private final DangerousClCommandValidator clValidator;

    public List<JobSchedule> list() {
        return scheduleMapper.selectList(new LambdaQueryWrapper<JobSchedule>()
                .orderByDesc(JobSchedule::getId));
    }

    
    public JobSchedule create(JobScheduleRequest request, String username) {
        JobSchedule schedule = new JobSchedule();
        apply(schedule, request);
        schedule.setStatus("PENDING");
        schedule.setCreatedBy(username);
        schedule.setCreatedTime(LocalDateTime.now());
        schedule.setUpdatedTime(LocalDateTime.now());
        scheduleMapper.insert(schedule);
        if (Boolean.TRUE.equals(schedule.getEnabled()) && !register(schedule)) {
            // T2：注册失败回写禁用标记并发告警——避免「DB enabled=true 但永不触发」的静默缺口
            disableAfterRegisterFailure(schedule);
        }
        return schedule;
    }

    
    public JobSchedule update(Long id, JobScheduleRequest request) {
        JobSchedule schedule = EntityUtil.require(id, "Job Schedule", scheduleMapper::selectById);
        apply(schedule, request);
        schedule.setUpdatedTime(LocalDateTime.now());
        scheduleMapper.updateById(schedule);
        if (Boolean.TRUE.equals(schedule.getEnabled())) {
            if (!register(schedule)) {
                disableAfterRegisterFailure(schedule);
                schedule.setEnabled(false);
            }
        } else {
            unregister(id);
        }
        return schedule;
    }

    
    public void delete(Long id) {
        EntityUtil.require(id, "Job Schedule", scheduleMapper::selectById);
        // T4：先落禁用守卫写再注销——若后续步骤失败，重启时不会把已删除任务复活（僵尸调度）
        scheduleMapper.update(null, new LambdaUpdateWrapper<JobSchedule>()
                .eq(JobSchedule::getId, id)
                .set(JobSchedule::getEnabled, false));
        unregister(id);
        historyMapper.delete(new LambdaQueryWrapper<JobScheduleHistory>()
                .eq(JobScheduleHistory::getScheduleId, id));
        scheduleMapper.deleteById(id);
    }

    
    public JobSchedule toggle(Long id, Boolean enabled) {
        JobSchedule schedule = EntityUtil.require(id, "Job Schedule", scheduleMapper::selectById);
        schedule.setEnabled(Boolean.TRUE.equals(enabled));
        schedule.setUpdatedTime(LocalDateTime.now());
        scheduleMapper.updateById(schedule);
        if (Boolean.TRUE.equals(schedule.getEnabled())) {
            if (!register(schedule)) {
                disableAfterRegisterFailure(schedule);
                schedule.setEnabled(false);
            }
        } else {
            unregister(id);
        }
        return schedule;
    }

    /** 手动立即执行（返回执行结果） */
    public ScheduleExecuteResultVO executeNow(Long id) {
        return execute(id);
    }

    /**
     * 实际执行（Quartz 触发与手动执行共用）。
     * C8：结果回写用针对性 UPDATE（仅 status/lastRunTime/lastResult/updatedTime）——
     * 全实体 updateById 会把并发期间管理员修改的 command/cron/enabled 用执行前旧值整体覆盖回去（丢失更新）。
     * 注：本项目为无事务架构，此处两条写语句各自原子，无「同事务」语义。
     */

    public ScheduleExecuteResultVO execute(Long id) {
        JobSchedule schedule = EntityUtil.require(id, "Job Schedule", scheduleMapper::selectById);
        long start = System.currentTimeMillis();
        String status = ExecutionStatus.SUCCESS;
        String message;
        try {
            AS400Client client = clientProvider.forServer(schedule.getServerId());
            if ("SQL".equalsIgnoreCase(schedule.getScheduleType())) {
                String sql = schedule.getCommand().trim();
                if (!SqlReadOnlyValidator.isReadOnly(sql)) {
                    throw new BusinessException(ErrorCode.SQL_READONLY_REQUIRED, "Only read-only SELECT/WITH queries are supported");
                }
                // S7：调度型 SQL 同样下推行数上限（内部任务取 1000 行足够聚合/校验用途）
                List<Map<String, Object>> rows = client.queryListCheckedBounded(sql, 1000);
                // W1：message 只存原始信息（无状态前缀/中文），成功/失败前缀由前端按 status 渲染
                message = rows.size() + " rows";
            } else {
                // S4：CL 分支高危动词兜底（保存时已校验，此处防历史数据/直改库绕过）
                clValidator.assertAllowed(schedule.getCommand());
                CommandResult result = client.execute(schedule.getCommand());
                if (!result.success()) {
                    throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, result.message());
                }
                message = result.message();
            }
        } catch (Exception e) {
            status = ExecutionStatus.FAILED;
            message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.error("[作业调度] 任务 {} 执行失败: {}", schedule.getName(), message);
            notifyFailure(schedule, message);
        }
        long costMs = System.currentTimeMillis() - start;
        // C8：针对性回写执行结果，不触碰 command/cron/enabled 等管理字段
        scheduleMapper.update(null, new LambdaUpdateWrapper<JobSchedule>()
                .eq(JobSchedule::getId, id)
                .set(JobSchedule::getStatus, status)
                .set(JobSchedule::getLastRunTime, LocalDateTime.now())
                .set(JobSchedule::getLastResult, truncate(message, 500))
                .set(JobSchedule::getUpdatedTime, LocalDateTime.now()));

        JobScheduleHistory history = new JobScheduleHistory();
        history.setScheduleId(id);
        history.setRunTime(LocalDateTime.now());
        history.setStatus(status);
        history.setMessage(truncate(message, 1000));
        history.setCostMs(costMs);
        historyMapper.insert(history);
        return new ScheduleExecuteResultVO(status, message, costMs);
    }

    public List<JobScheduleHistory> history(Long scheduleId) {
        return historyMapper.selectList(new LambdaQueryWrapper<JobScheduleHistory>()
                .eq(JobScheduleHistory::getScheduleId, scheduleId)
                .orderByDesc(JobScheduleHistory::getRunTime)
                .last(PageConstants.limitClause(50)));
    }

    /** 启动时重新注册启用中的任务（Quartz 重启不丢） */
    @Override
    public void run(ApplicationArguments args) {
        List<JobSchedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<JobSchedule>().eq(JobSchedule::getEnabled, true));
        for (JobSchedule schedule : schedules) {
            register(schedule);
        }
        if (!schedules.isEmpty()) {
            log.info("[作业调度] 已恢复 {} 个调度任务", schedules.size());
        }
    }

    /** 复用监控告警通道（rx_alert_event + 日志）通知调度失败 */
    private void notifyFailure(JobSchedule schedule, String message) {
        try {
            ScheduleAlertEvent event = new ScheduleAlertEvent();
            event.setInstanceId(schedule.getServerId());
            event.setRuleId(null);
            event.setLevel("CRITICAL");
            event.setMessage("Job schedule [" + schedule.getName() + "] execution failed: " + message);
            event.setStatus("OPEN");
            event.setCreatedTime(LocalDateTime.now());
            alertEventMapper.insert(event);
            // 发布告警事件（app 模块监听 → Webhook 推送），title 用英文 code 保持中性
            eventPublisher.publishEvent(new AlertRaisedEvent("CRITICAL", "JOB_SCHEDULE",
                    event.getMessage(), schedule.getServerId()));
        } catch (Exception e) {
            log.warn("[作业调度] 失败告警写入失败: {}", e.getMessage());
        }
    }



    private void apply(JobSchedule schedule, JobScheduleRequest request) {
        // P2-13：保存时即校验 cron，避免无效表达式静默注册失败（任务 enabled 但永不触发）
        String cronError = CronValidator.validate(request.getCronExpr());
        if (cronError != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, cronError);
        }
        String type = request.getScheduleType() == null ? "" : request.getScheduleType().trim().toUpperCase();
        String command = request.getCommand() == null ? "" : request.getCommand().trim();
        // S3：保存时即校验命令——与 SQL 类型的只读校验对称，CL 不再裸奔到执行期
        if ("SQL".equals(type)) {
            if (!SqlReadOnlyValidator.isReadOnly(command)) {
                throw new BusinessException(ErrorCode.SQL_READONLY_REQUIRED, "SQL-type tasks only support read-only SELECT/WITH queries");
            }
        } else {
            if (command.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "CL command is required");
            }
            if (command.length() > 500) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "CL command must not exceed 500 characters");
            }
            if (command.matches(".*[\\r\\n\\u0000].*")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "CL command must not contain newlines or null characters");
            }
        }
        schedule.setName(request.getName());
        schedule.setDescription(request.getDescription());
        schedule.setServerId(request.getServerId());
        schedule.setScheduleType(type);
        schedule.setCommand(command);
        schedule.setCronExpr(request.getCronExpr().trim());
        schedule.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    /** T2：注册失败回写禁用并发 CRITICAL 告警，消除「enabled 但永不触发」静默缺口 */
    private void disableAfterRegisterFailure(JobSchedule schedule) {
        try {
            scheduleMapper.update(null, new LambdaUpdateWrapper<JobSchedule>()
                    .eq(JobSchedule::getId, schedule.getId())
                    .set(JobSchedule::getEnabled, false)
                    .set(JobSchedule::getUpdatedTime, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("[作业调度] 注册失败后回写禁用标记失败: id={}", schedule.getId(), e);
        }
        try {
            ScheduleAlertEvent event = new ScheduleAlertEvent();
            event.setInstanceId(schedule.getServerId());
            event.setLevel("CRITICAL");
            event.setMessage("Job schedule [" + schedule.getName()
                    + "] Quartz register failed, task disabled: cron=" + schedule.getCronExpr());
            event.setStatus("OPEN");
            event.setCreatedTime(LocalDateTime.now());
            alertEventMapper.insert(event);
            eventPublisher.publishEvent(new AlertRaisedEvent("CRITICAL", "JOB_SCHEDULE",
                    event.getMessage(), schedule.getServerId()));
        } catch (Exception e) {
            log.warn("[作业调度] 注册失败告警写入失败: {}", e.getMessage());
        }
    }

    /**
     * 注册任务到调度器。T2：返回是否成功——失败由调用方决定禁用回写；
     * 启动期 run() 的重注册失败仅记日志（下次重启自动重试）。
     */
    private boolean register(JobSchedule schedule) {
        try {
            JobKey jobKey = JobKey.jobKey("schedule-" + schedule.getId());
            JobDetail detail = JobBuilder.newJob(ScheduleQuartzJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("scheduleId", schedule.getId())
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(TriggerKey.triggerKey("schedule-" + schedule.getId()))
                    .forJob(jobKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(schedule.getCronExpr())
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
            if (scheduler.checkExists(jobKey)) {
                // 已存在（更新 cron / 重启后重注册）：仅替换 trigger，JobDetail 不变
                scheduler.rescheduleJob(trigger.getKey(), trigger);
            } else {
                // 双参版本一次注册 job + trigger（无需 durable）
                scheduler.scheduleJob(detail, trigger);
            }
            return true;
        } catch (SchedulerException | RuntimeException e) {
            log.error("[作业调度] 注册任务 {} 失败: {}", schedule.getName(), e.getMessage(), e);
            return false;
        }
    }

    private void unregister(Long id) {
        try {
            scheduler.deleteJob(JobKey.jobKey("schedule-" + id));
        } catch (SchedulerException e) {
            log.warn("[作业调度] 注销任务 {} 失败: {}", id, e.getMessage());
        }
    }
}