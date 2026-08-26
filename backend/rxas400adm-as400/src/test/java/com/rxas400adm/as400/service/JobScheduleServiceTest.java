package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.springframework.context.ApplicationEventPublisher;

import com.rxas400adm.as400.vo.ScheduleExecuteResultVO;
import com.rxas400adm.common.security.DangerousClCommandValidator;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobScheduleServiceTest {

    /**
     * C8/T4 后服务内构造 LambdaUpdateWrapper/LambdaQueryWrapper，
     * Mockito 环境无 MyBatis-Plus 启动上下文，需为涉及实体初始化 TableInfo 缓存
     * （范式同 system 模块 SqlInjectionTest.initTableInfo）。
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, JobSchedule.class);
        TableInfoHelper.initTableInfo(assistant, JobScheduleHistory.class);
    }

    @Mock
    private JobScheduleMapper scheduleMapper;

    @Mock
    private JobScheduleHistoryMapper historyMapper;

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    @Mock
    private Scheduler scheduler;

    @Mock
    private ScheduleAlertEventMapper alertEventMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    /** S4：构造签名适配——mock 校验器默认放行（doNothing） */
    @Mock
    private DangerousClCommandValidator clValidator;

    private JobScheduleService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new JobScheduleService(scheduleMapper, historyMapper, clientProvider,
                scheduler, alertEventMapper, eventPublisher, clValidator);
        lenient().when(clientProvider.forServer(1L)).thenReturn(client);
        lenient().when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(false);
    }

    private JobScheduleRequest sqlRequest() {
        JobScheduleRequest req = new JobScheduleRequest();
        req.setName("每日作业报表");
        req.setServerId(1L);
        req.setScheduleType("SQL");
        req.setCommand("SELECT * FROM QSYS2.SYSTEM_STATUS_INFO");
        req.setCronExpr("0 0 6 * * ?");
        req.setEnabled(true);
        return req;
    }

    @Test
    void create_shouldInsertAndRegisterQuartz() throws Exception {
        when(scheduleMapper.insert(any(JobSchedule.class))).thenAnswer(inv -> {
            ((JobSchedule) inv.getArgument(0)).setId(1L);
            return 1;
        });
        JobSchedule created = service.create(sqlRequest(), "admin");
        assertEquals("SQL", created.getScheduleType());
        verify(scheduleMapper).insert(any(JobSchedule.class));
        verify(scheduler).scheduleJob(any(org.quartz.JobDetail.class), any(org.quartz.Trigger.class));
    }

    @Test
    void create_disabled_shouldNotRegister() throws Exception {
        JobScheduleRequest req = sqlRequest();
        req.setEnabled(false);
        service.create(req, "admin");
        verify(scheduler, never()).scheduleJob(any(org.quartz.JobDetail.class), any(org.quartz.Trigger.class));
    }

    @Test
    void execute_sqlSuccess_shouldWriteHistoryAndUpdateLastRun() {
        JobSchedule schedule = scheduleEntity();
        when(scheduleMapper.selectById(1L)).thenReturn(schedule);
        when(client.queryListCheckedBounded(any(), anyInt()))
                .thenReturn(List.of(Map.of("A", 1), Map.of("A", 2)));

        ScheduleExecuteResultVO result = service.execute(1L);

        assertEquals("SUCCESS", result.status());
        // C8：结果走针对性 UPDATE（不再回写内存实体），验证其发生即可
        verify(scheduleMapper).update(isNull(), any());
        ArgumentCaptor<JobScheduleHistory> captor = ArgumentCaptor.forClass(JobScheduleHistory.class);
        verify(historyMapper).insert(captor.capture());
        assertEquals("SUCCESS", captor.getValue().getStatus());
        assertEquals(1L, captor.getValue().getScheduleId());
    }

    @Test
    void execute_nonSelectSql_shouldFail() {
        JobSchedule schedule = scheduleEntity();
        schedule.setCommand("DELETE FROM X");
        when(scheduleMapper.selectById(1L)).thenReturn(schedule);

        ScheduleExecuteResultVO result = service.execute(1L);

        assertEquals("FAILED", result.status());
        verify(client, never()).queryList(any());
        // 失败应写告警事件（复用 rx_alert_event 通道）
        verify(alertEventMapper).insert(any(ScheduleAlertEvent.class));
    }

    @Test
    void execute_success_shouldNotWriteAlert() {
        JobSchedule schedule = scheduleEntity();
        when(scheduleMapper.selectById(1L)).thenReturn(schedule);
        when(client.queryListCheckedBounded(any(), anyInt()))
                .thenReturn(List.of(Map.of("A", 1)));

        service.execute(1L);

        verify(alertEventMapper, never()).insert(any(ScheduleAlertEvent.class));
    }

    @Test
    void execute_clFailure_shouldRecordFailedHistory() {
        JobSchedule schedule = scheduleEntity();
        schedule.setScheduleType("CL");
        schedule.setCommand("WRKACTJOB");
        when(scheduleMapper.selectById(1L)).thenReturn(schedule);
        when(client.execute("WRKACTJOB")).thenReturn(CommandResult.fail("command error"));

        ScheduleExecuteResultVO result = service.execute(1L);

        assertEquals("FAILED", result.status());
        assertTrue(result.message().contains("command error"));
    }

    @Test
    void delete_shouldUnregisterAndCleanHistory() throws Exception {
        when(scheduleMapper.selectById(1L)).thenReturn(scheduleEntity());
        service.delete(1L);
        verify(scheduler).deleteJob(any(org.quartz.JobKey.class));
        verify(historyMapper).delete(any());
        verify(scheduleMapper).deleteById(1L);
    }

    private JobSchedule scheduleEntity() {
        JobSchedule schedule = new JobSchedule();
        schedule.setId(1L);
        schedule.setName("每日作业报表");
        schedule.setServerId(1L);
        schedule.setScheduleType("SQL");
        schedule.setCommand("SELECT * FROM QSYS2.SYSTEM_STATUS_INFO");
        schedule.setCronExpr("0 0 6 * * ?");
        schedule.setEnabled(true);
        schedule.setStatus("PENDING");
        return schedule;
    }
}
