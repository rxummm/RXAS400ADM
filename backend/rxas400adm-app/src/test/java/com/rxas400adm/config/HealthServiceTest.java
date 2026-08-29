package com.rxas400adm.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.monitor.alert.AlertEvent;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;

import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, IbmiSystem.class);
        TableInfoHelper.initTableInfo(assistant, AlertEvent.class);
    }

    @Mock private IbmiSystemMapper systemMapper;
    @Mock private AlertEventMapper alertEventMapper;
    @Mock private AS400ClientProvider clientProvider;
    @Mock private Scheduler scheduler;
    @Mock private Executor healthProbePool;

    private HealthService service;

    @BeforeEach
    void setUp() {
        service = new HealthService(systemMapper, alertEventMapper, clientProvider, scheduler, healthProbePool);
    }

    @Test
    @DisplayName("probeDatabase → 正常返回 true")
    void probeDatabase_ok_shouldReturnTrue() {
        when(systemMapper.selectCount(null)).thenReturn(5L);
        assertTrue(service.probeDatabase());
    }

    @Test
    @DisplayName("probeDatabase → 异常返回 false")
    void probeDatabase_error_shouldReturnFalse() {
        when(systemMapper.selectCount(null)).thenThrow(new RuntimeException("DB down"));
        assertFalse(service.probeDatabase());
    }

    @Test
    @DisplayName("countOpenAlerts → 返回 OPEN 告警数")
    void countOpenAlerts_shouldReturn() {
        when(alertEventMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        assertEquals(3, service.countOpenAlerts());
    }

    @Test
    @DisplayName("listSystemsOrdered → 返回服务器列表")
    void listSystemsOrdered_shouldReturn() {
        IbmiSystem sys = new IbmiSystem();
        sys.setId(1L);
        sys.setName("PROD400");
        when(systemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sys));

        List<IbmiSystem> result = service.listSystemsOrdered();
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("probeServers → 无服务器返回空列表")
    void probeServers_empty_shouldReturnEmpty() {
        when(systemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        assertTrue(service.probeServers().isEmpty());
    }
}
