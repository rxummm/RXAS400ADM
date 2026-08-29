package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.dto.CalendarEventDTO;
import com.rxas400adm.system.entity.CalendarEvent;
import com.rxas400adm.system.mapper.CalendarEventMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarEventServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, CalendarEvent.class);
    }

    @Mock
    private CalendarEventMapper eventMapper;

    private CalendarEventService service;

    @BeforeEach
    void setUp() {
        service = new CalendarEventService(eventMapper);
    }

    @Test
    @DisplayName("month → 返回当月事件列表")
    void month_shouldReturnList() {
        when(eventMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        List<CalendarEvent> result = service.month(2026, 8, 1L);
        assertNotNull(result);
    }

    @Test
    @DisplayName("create → 正常新增")
    void create_shouldInsert() {
        when(eventMapper.insert(any(CalendarEvent.class))).thenAnswer(inv -> {
            inv.getArgument(0, CalendarEvent.class).setId(1L);
            return 1;
        });

        CalendarEventDTO dto = new CalendarEventDTO();
        dto.setTitle("变更窗口");
        dto.setEventDate(LocalDate.of(2026, 8, 28));

        CalendarEvent result = service.create(dto, 1L);
        assertEquals("变更窗口", result.getTitle());
        assertEquals(1L, result.getUserId());
    }

    @Test
    @DisplayName("delete → 非本人事件抛异常")
    void delete_notOwner_shouldThrow() {
        CalendarEvent event = new CalendarEvent();
        event.setId(1L);
        event.setUserId(2L);
        when(eventMapper.selectById(1L)).thenReturn(event);

        assertThrows(BusinessException.class, () -> service.delete(1L, 1L));
    }

    @Test
    @DisplayName("delete → 本人事件正常删除")
    void delete_owner_shouldDelete() {
        CalendarEvent event = new CalendarEvent();
        event.setId(1L);
        event.setUserId(1L);
        when(eventMapper.selectById(1L)).thenReturn(event);

        service.delete(1L, 1L);
        verify(eventMapper).deleteById(1L);
    }
}