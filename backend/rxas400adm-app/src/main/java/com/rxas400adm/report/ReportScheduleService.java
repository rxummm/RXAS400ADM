package com.rxas400adm.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rxas400adm.as400.util.CronValidator;
import com.rxas400adm.common.constants.ExecutionStatus;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.email.MailMessage;
import com.rxas400adm.email.service.IEmailService;
import com.rxas400adm.report.dto.ReportScheduleDTO;
import com.rxas400adm.report.mapper.ReportScheduleHistoryMapper;
import com.rxas400adm.report.mapper.ReportScheduleMapper;
import com.rxas400adm.report.vo.ScheduleExecuteResultVO;
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
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表定时任务（2.5.18）：按 cron 生成 PDF/Excel 并通过邮件通道推送。
 * 任务持久化在 rx_report_schedule，Quartz（RAMJobStore）触发；
 * 启动时自动恢复启用中的任务（与 JobScheduleService 同一模式）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportScheduleService implements IReportScheduleService, ApplicationRunner {

    private final ReportScheduleMapper scheduleMapper;
    private final ReportScheduleHistoryMapper historyMapper;
    private final IReportService reportService;
    private final IEmailService emailService;
    private final Scheduler scheduler;

    public List<ReportSchedule> list() {
        return scheduleMapper.selectList(new LambdaQueryWrapper<ReportSchedule>()
                .orderByDesc(ReportSchedule::getId));
    }

    
    public ReportSchedule create(ReportScheduleDTO dto, String username) {
        ReportSchedule schedule = toEntity(dto);
        applyDefaults(schedule);
        schedule.setStatus("PENDING");
        schedule.setCreatedBy(username);
        schedule.setCreatedTime(LocalDateTime.now());
        schedule.setUpdatedTime(LocalDateTime.now());
        scheduleMapper.insert(schedule);
        if (Boolean.TRUE.equals(schedule.getEnabled()) && !register(schedule)) {
            // T2：注册失败回写禁用标记，避免「enabled 但永不触发」静默缺口
            disableAfterRegisterFailure(schedule);
            schedule.setEnabled(false);
        }
        return schedule;
    }

    
    public ReportSchedule update(Long id, ReportScheduleDTO dto) {
        EntityUtil.require(id, "报表定时任务", scheduleMapper::selectById);
        ReportSchedule schedule = toEntity(dto);
        applyDefaults(schedule);
        schedule.setId(id);
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
        EntityUtil.require(id, "报表定时任务", scheduleMapper::selectById);
        // T4：先落禁用守卫写再注销——防止后续失败时重启复活已删除任务
        scheduleMapper.update(null, new LambdaUpdateWrapper<ReportSchedule>()
                .eq(ReportSchedule::getId, id)
                .set(ReportSchedule::getEnabled, false));
        unregister(id);
        historyMapper.delete(new LambdaQueryWrapper<ReportScheduleHistory>()
                .eq(ReportScheduleHistory::getScheduleId, id));
        scheduleMapper.deleteById(id);
    }

    
    public ReportSchedule toggle(Long id, Boolean enabled) {
        ReportSchedule schedule = EntityUtil.require(id, "报表定时任务", scheduleMapper::selectById);
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
     * 实际执行：生成报表字节 → 写历史 → 邮件推送附件。
     * C8：结果回写针对性 UPDATE（仅状态/时间/结果字段），避免并发丢失更新；
     * 无事务架构：两条写语句各自原子。
     */

    public ScheduleExecuteResultVO execute(Long id) {
        ReportSchedule schedule = EntityUtil.require(id, "报表定时任务", scheduleMapper::selectById);
        long start = System.currentTimeMillis();
        String status = ExecutionStatus.SUCCESS;
        String message;
        long fileBytes = 0;
        try {
            int days = schedule.getDays() == null ? 7 : schedule.getDays();
            byte[] data = reportService.generate(schedule.getReportType(), schedule.getFormat(),
                    schedule.getServerId(), days);
            if (data.length == 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "报表生成失败（数据为空或 PDF 字体不可用）");
            }
            fileBytes = data.length;
            String ext = "pdf".equalsIgnoreCase(schedule.getFormat()) ? "pdf" : "xlsx";
            String filename = "report-" + schedule.getReportType() + "-" + LocalDate.now() + "." + ext;
            String title = "RXAS400 定时报表：" + schedule.getName();
            String text = "任务「" + schedule.getName() + "」已按计划生成，见附件（"
                    + (fileBytes / 1024.0 >= 1024
                        ? String.format("%.1f MB", fileBytes / 1024.0 / 1024.0)
                        : String.format("%.1f KB", fileBytes / 1024.0))
                    + "）。\n类型：" + schedule.getReportType() + "，格式：" + ext;
            emailService.send(MailMessage.report(title, text, schedule.getRecipients(), filename, data));
            message = "已生成并推送邮件附件（" + fileBytes + " 字节）";
        } catch (Exception e) {
            status = ExecutionStatus.FAILED;
            message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            // 【E5-3】追加异常对象，保留完整堆栈（原仅拼 getMessage 丢堆栈）
            log.error("[报表定时] 任务 {} 执行失败: {}", schedule.getName(), message, e);
        }
        long costMs = System.currentTimeMillis() - start;
        // C8：针对性回写执行结果
        scheduleMapper.update(null, new LambdaUpdateWrapper<ReportSchedule>()
                .eq(ReportSchedule::getId, id)
                .set(ReportSchedule::getStatus, status)
                .set(ReportSchedule::getLastRunTime, LocalDateTime.now())
                .set(ReportSchedule::getLastResult, truncate(status + ": " + message, 500))
                .set(ReportSchedule::getUpdatedTime, LocalDateTime.now()));

        ReportScheduleHistory history = new ReportScheduleHistory();
        history.setScheduleId(id);
        history.setRunTime(LocalDateTime.now());
        history.setStatus(status);
        history.setMessage(truncate(message, 1000));
        history.setFileBytes(fileBytes);
        history.setCreatedTime(LocalDateTime.now());
        historyMapper.insert(history);
        log.info("[报表定时] 任务 {} 执行完成 {}（{}ms）", schedule.getName(), status, costMs);
        return new ScheduleExecuteResultVO(status, message, fileBytes);
    }

    public List<ReportScheduleHistoryVO> history(Long scheduleId) {
        return historyMapper.selectList(new LambdaQueryWrapper<ReportScheduleHistory>()
                .eq(ReportScheduleHistory::getScheduleId, scheduleId)
                .orderByDesc(ReportScheduleHistory::getRunTime)
                .last(PageConstants.limitClause(50)))
                .stream().map(ReportScheduleHistoryVO::from).toList();
    }

    /** 启动时恢复启用中的定时任务 */
    @Override
    public void run(ApplicationArguments args) {
        List<ReportSchedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<ReportSchedule>().eq(ReportSchedule::getEnabled, true));
        for (ReportSchedule schedule : schedules) {
            register(schedule);
        }
        if (!schedules.isEmpty()) {
            log.info("[报表定时] 已恢复 {} 个定时任务", schedules.size());
        }
    }



    /** DTO → 实体映射（仅业务字段；id/status/审计字段由服务端托管） */
    private ReportSchedule toEntity(ReportScheduleDTO dto) {
        ReportSchedule schedule = new ReportSchedule();
        schedule.setName(dto.getName());
        schedule.setReportType(dto.getReportType());
        schedule.setFormat(dto.getFormat());
        schedule.setServerId(dto.getServerId());
        schedule.setDays(dto.getDays());
        schedule.setCronExpr(dto.getCronExpr());
        schedule.setRecipients(dto.getRecipients());
        schedule.setEnabled(dto.getEnabled());
        return schedule;
    }

    private void applyDefaults(ReportSchedule schedule) {
        if (schedule.getName() == null || schedule.getName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务名称不能为空");
        }
        if (schedule.getReportType() == null || schedule.getReportType().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "报表类型不能为空");
        }
        if (schedule.getCronExpr() == null || schedule.getCronExpr().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cron 表达式不能为空");
        }
        // P2-13：保存时即校验 cron，避免无效表达式静默注册失败（任务 enabled 但永不触发）
        String cronError = CronValidator.validate(schedule.getCronExpr());
        if (cronError != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, cronError);
        }
        schedule.setCronExpr(schedule.getCronExpr().trim());
        if (schedule.getFormat() == null || schedule.getFormat().isBlank()) {
            schedule.setFormat("xlsx");
        }
        if (schedule.getDays() == null) {
            schedule.setDays(7);
        }
        if (schedule.getEnabled() == null) {
            schedule.setEnabled(Boolean.TRUE);
        }
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    /** T2：注册失败回写禁用标记（报表任务无告警通道，仅日志留痕） */
    private void disableAfterRegisterFailure(ReportSchedule schedule) {
        try {
            scheduleMapper.update(null, new LambdaUpdateWrapper<ReportSchedule>()
                    .eq(ReportSchedule::getId, schedule.getId())
                    .set(ReportSchedule::getEnabled, false)
                    .set(ReportSchedule::getUpdatedTime, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("[报表定时] 注册失败后回写禁用标记失败: id={}", schedule.getId(), e);
        }
    }

    /** T2：返回注册是否成功——失败由调用方决定禁用回写；启动期 run() 失败仅记日志（下次重启重试） */
    private boolean register(ReportSchedule schedule) {
        try {
            JobKey jobKey = JobKey.jobKey("report-schedule-" + schedule.getId());
            JobDetail detail = JobBuilder.newJob(ReportScheduleQuartzJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("scheduleId", schedule.getId())
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(TriggerKey.triggerKey("report-schedule-" + schedule.getId()))
                    .forJob(jobKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(schedule.getCronExpr())
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
            if (scheduler.checkExists(jobKey)) {
                scheduler.rescheduleJob(trigger.getKey(), trigger);
            } else {
                scheduler.scheduleJob(detail, trigger);
            }
            return true;
        } catch (SchedulerException | RuntimeException e) {
            log.error("[报表定时] 注册任务 {} 失败: {}", schedule.getName(), e.getMessage(), e);
            return false;
        }
    }

    private void unregister(Long id) {
        try {
            scheduler.deleteJob(JobKey.jobKey("report-schedule-" + id));
        } catch (SchedulerException e) {
            log.warn("[报表定时] 注销任务 {} 失败: {}", id, e.getMessage());
        }
    }
}
