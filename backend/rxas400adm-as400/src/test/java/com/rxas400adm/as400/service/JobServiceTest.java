package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.vo.JobInfo;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @BeforeAll
    static void initSqlRegistry() throws Exception {
        // 通过反射调用 SqlStatementRegistry.load() 初始化静态单例，加载 classpath:sql/as400-*.xml
        SqlStatementRegistry registry = new SqlStatementRegistry();
        java.lang.reflect.Method loadMethod = SqlStatementRegistry.class.getDeclaredMethod("load");
        loadMethod.setAccessible(true);
        loadMethod.invoke(registry);
    }

    @Mock
    private AS400ClientProvider clientProvider;

    @Mock
    private AS400Client client;

    @Mock
    private ProfileResolver profileResolver;

    @Mock
    private ExecutorService msgwExecutor;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(clientProvider, profileResolver, msgwExecutor);
        // S8：负面用例在触达 client 前即抛异常，current() 打桩需 lenient 避免 strict-stubs 误报
        lenient().when(clientProvider.current()).thenReturn(client);
        // 模拟 ExecutorService 同步执行任务，避免 CompletableFuture.supplyAsync 挂起
        lenient().doAnswer(inv -> {
            Runnable task = inv.getArgument(0);
            task.run();
            return null;
        }).when(msgwExecutor).execute(any(Runnable.class));
    }

    @Test
    void activeJobs_shouldMapRowsToJobInfo() {
        when(client.queryList(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(
                Map.of("JOB_NAME", "JOB1001", "JOB_USER", "QSECOFR", "JOB_NUMBER", "123456",
                        "JOB_STATUS", "RUN", "JOB_PROGRAM", "QRPGLESRC", "CPU_TIME", 12L,
                        "TEMPORARY_STORAGE", 1024L)
        ));
        List<JobInfo> jobs = jobService.activeJobs(null);
        assertFalse(jobs.isEmpty());
        JobInfo job = jobs.get(0);
        assertEquals("JOB1001", job.getJobName());
        assertEquals("QSECOFR", job.getJobUser());
        assertEquals("RUN", job.getJobStatus());
        assertEquals("12", job.getCpuTime());
    }

    @Test
    void msgwJobs_shouldPassStatusFilter() {
        when(client.queryList(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                Map.of("JOB_NAME", "JOB2001", "JOB_USER", "QPGMR", "JOB_STATUS", "MSGW")
        ));
        List<JobInfo> jobs = jobService.msgwJobs();
        assertEquals(1, jobs.size());
        assertEquals("MSGW", jobs.get(0).getJobStatus());
    }

    @Test
    void endJob_shouldExecuteEndJobCommand() {
        when(client.execute(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(CommandResult.ok("模拟执行成功: ENDJOB"));
        CommandResult result = jobService.endJob("JOB1001", "QSECOFR", "123456");
        assertTrue(result.success());
    }

    @Test
    void holdJob_shouldExecuteHoldJobCommand() {
        when(client.execute(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(CommandResult.ok("模拟执行成功: HLDJOB"));
        assertTrue(jobService.holdJob("JOB1001", "QSECOFR", "123456").success());
    }

    @Test
    void releaseJob_shouldExecuteReleaseJobCommand() {
        when(client.execute(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(CommandResult.ok("模拟执行成功: RLSJOB"));
        assertTrue(jobService.releaseJob("JOB1001", "QSECOFR", "123456").success());
    }

    @Test
    void jobLog_shouldQueryJoblogInfo() {
        when(client.queryList(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                Map.of("ORDINAL_POSITION", 1, "MESSAGE_ID", "CPF1241", "MESSAGE_TEXT", "Job started")
        ));
        List<Map<String, Object>> log = jobService.jobLog("JOB1001", "QSECOFR", "123456");
        assertFalse(log.isEmpty());
        assertEquals("CPF1241", log.get(0).get("MESSAGE_ID"));
    }

    @Test
    void msgwMessages_shouldReturnJobWithMessage() {
        when(client.queryList(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("ACTIVE_JOB_INFO") && sql.contains("JOB_STATUS")) {
                return List.of(Map.of("JOB_NAME", "JOB2001", "JOB_USER", "QPGMR", "JOB_NUMBER", "111", "JOB_STATUS", "MSGW"));
            }
            if (sql.contains("MESSAGE_QUEUE_INFO")) {
                return List.of(Map.of("MESSAGE_ID", "CPA0701", "MESSAGE_TYPE", "INQUIRY",
                        "MESSAGE_TEXT", "Reply", "REPLY_STATUS", "MSGW"));
            }
            return List.of();
        });
        List<Map<String, Object>> msgs = jobService.msgwMessages();
        assertEquals(1, msgs.size());
        assertEquals("CPA0701", msgs.get(0).get("MESSAGE_ID"));
    }

    @Test
    void replyMsg_shouldExecuteReplyCommand() {
        when(client.execute(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(CommandResult.ok("模拟执行成功: RPLMSG"));
        assertTrue(jobService.replyMsg("JOB1001", "QSECOFR", "123456").success());
    }

    // ---------- S8：负面用例（非法作业标识必须在触达 client 前被拒绝） ----------

    @Test
    void endJob_shouldRejectJobNameWithSpaceAndParen() {
        // S8：作业名含空格+右括号（可闭合 JOB(...) 注入额外参数）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> jobService.endJob("X Y)", "QSECOFR", "123456"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(client, never()).execute(any());
    }

    @Test
    void endJob_shouldRejectJobNameWithSingleQuote() {
        // S8：作业名含单引号（CL 字符串字面量逃逸向量）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> jobService.endJob("A'B", "QSECOFR", "123456"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(client, never()).execute(any());
    }

    @Test
    void endJob_shouldRejectJobNameWithSemicolon() {
        // S8：作业名含分号（多语句拼接注入向量）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> jobService.endJob("JOB;DLTLIB LIB(QTEMP)", "QSECOFR", "123456"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(client, never()).execute(any());
    }

    @Test
    void endJob_shouldRejectNonNumericJobNumber() {
        // S8：作业号必须是 1~6 位十进制，"12A3" 直接拒绝
        BusinessException ex = assertThrows(BusinessException.class,
                () -> jobService.endJob("JOB1001", "QSECOFR", "12A3"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(client, never()).execute(any());
    }
}
