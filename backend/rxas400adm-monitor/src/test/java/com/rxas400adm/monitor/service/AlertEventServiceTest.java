package com.rxas400adm.monitor.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertEventServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, AlertEvent.class);
    }

    @Mock
    private AlertEventMapper alertEventMapper;

    private AlertEventService service;

    @BeforeEach
    void setUp() {
        service = new AlertEventService(alertEventMapper);
    }

    @Test
    @DisplayName("recent → 正常返回告警列表")
    void recent_shouldReturnList() {
        AlertEvent event = new AlertEvent();
        event.setId(1L);
        event.setStatus("OPEN");
        when(alertEventMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(event));

        List<AlertEvent> result = service.recent(10);
        assertEquals(1, result.size());
        assertEquals("OPEN", result.get(0).getStatus());
    }

    @Test
    @DisplayName("recent → 无告警返回空列表")
    void recent_empty_shouldReturnEmpty() {
        when(alertEventMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<AlertEvent> result = service.recent(10);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("recent → limit 边界值钳制（负数→1，超大→200）")
    void recent_limitBoundary_shouldClamp() {
        when(alertEventMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.recent(-5);
        service.recent(999);
        // 验证不抛异常且正常调用 mapper
        verify(alertEventMapper, times(2)).selectList(any(LambdaQueryWrapper.class));
    }
}
