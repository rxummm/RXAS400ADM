package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.dto.JobSlaDTO;
import com.rxas400adm.as400.entity.JobSla;
import com.rxas400adm.as400.mapper.JobSlaMapper;
import com.rxas400adm.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobSlaService 测试")
class JobSlaServiceTest {

    @Mock
    private JobSlaMapper slaMapper;
    @Mock
    private AS400ClientProvider clientProvider;

    private JobSlaService service;

    @BeforeEach
    void setUp() {
        service = new JobSlaService(slaMapper, clientProvider);
    }

    @Test
    @DisplayName("list() — 返回全部 SLA 规则")
    void list_returnsAll() {
        JobSla sla = createSampleSla();
        when(slaMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sla));

        List<JobSla> result = service.list();

        assertEquals(1, result.size());
        verify(slaMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("create() — 正常创建")
    void create_normal() {
        JobSlaDTO dto = new JobSlaDTO();
        dto.setJobName("MYJOB");
        dto.setScheduleName("MYSCHED");
        dto.setExpectedDurationSec(300);
        dto.setDeviationPercent(20);
        dto.setEnabled(true);
        when(slaMapper.insert(any(JobSla.class))).thenReturn(1);

        JobSla result = service.create(dto, "admin");

        assertNotNull(result);
        assertEquals("MYJOB", result.getJobName());
        assertEquals(300, result.getExpectedDurationSec());
    }

    @Test
    @DisplayName("create() — 作业名为空抛异常")
    void create_blankJobName_throws() {
        JobSlaDTO dto = new JobSlaDTO();
        dto.setJobName("");
        dto.setExpectedDurationSec(300);
        assertThrows(BusinessException.class, () -> service.create(dto, "admin"));
    }

    @Test
    @DisplayName("create() — 预期耗时 <=0 抛异常")
    void create_invalidDuration_throws() {
        JobSlaDTO dto = new JobSlaDTO();
        dto.setJobName("MYJOB");
        dto.setExpectedDurationSec(0);
        assertThrows(BusinessException.class, () -> service.create(dto, "admin"));
    }

    @Test
    @DisplayName("create() — 默认启用 + 偏差 20%")
    void create_defaults() {
        JobSlaDTO dto = new JobSlaDTO();
        dto.setJobName("MYJOB");
        dto.setExpectedDurationSec(300);
        when(slaMapper.insert(any(JobSla.class))).thenReturn(1);

        service.create(dto, "admin");

        ArgumentCaptor<JobSla> captor = ArgumentCaptor.forClass(JobSla.class);
        verify(slaMapper).insert(captor.capture());
        assertTrue(captor.getValue().getEnabled());
        assertEquals(20, captor.getValue().getDeviationPercent());
    }

    @Test
    @DisplayName("update() — 存在则更新")
    void update_exists() {
        JobSla existing = createSampleSla();
        existing.setId(1L);
        when(slaMapper.selectById(1L)).thenReturn(existing);
        when(slaMapper.updateById(any(JobSla.class))).thenReturn(1);

        JobSlaDTO dto = new JobSlaDTO();
        dto.setJobName("NEWJOB");
        dto.setExpectedDurationSec(600);
        JobSla result = service.update(1L, dto);

        assertEquals("NEWJOB", result.getJobName());
        assertEquals(600, result.getExpectedDurationSec());
    }

    @Test
    @DisplayName("update() — 不存在抛异常")
    void update_notExists_throws() {
        when(slaMapper.selectById(99L)).thenReturn(null);
        JobSlaDTO dto = new JobSlaDTO();
        dto.setJobName("JOB");
        dto.setExpectedDurationSec(100);
        assertThrows(BusinessException.class, () -> service.update(99L, dto));
    }

    @Test
    @DisplayName("delete() — 存在则删除")
    void delete_exists() {
        when(slaMapper.selectById(1L)).thenReturn(createSampleSla());
        when(slaMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> service.delete(1L));
        verify(slaMapper).deleteById(1L);
    }

    @Test
    @DisplayName("delete() — 不存在抛异常")
    void delete_notExists_throws() {
        when(slaMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.delete(99L));
    }

    @Test
    @DisplayName("executions() — 空列表返回空")
    void executions_empty() {
        AS400Client mockClient = mock(AS400Client.class);
        when(clientProvider.current()).thenReturn(mockClient);
        when(mockClient.jobSlaExecutions()).thenReturn(List.of());

        List<Map<String, Object>> result = service.executions();

        assertTrue(result.isEmpty());
    }

    private JobSla createSampleSla() {
        JobSla sla = new JobSla();
        sla.setId(1L);
        sla.setJobName("MYJOB");
        sla.setScheduleName("MYSCHED");
        sla.setExpectedDurationSec(300);
        sla.setDeviationPercent(20);
        sla.setEnabled(true);
        sla.setCreatedTime(LocalDateTime.now());
        sla.setUpdatedTime(LocalDateTime.now());
        return sla;
    }
}