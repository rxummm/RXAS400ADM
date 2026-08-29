package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.system.entity.DashboardWidget;
import com.rxas400adm.system.mapper.DashboardWidgetMapper;
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
class DashboardWidgetServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DashboardWidget.class);
    }

    @Mock
    private DashboardWidgetMapper widgetMapper;

    private DashboardWidgetService service;

    @BeforeEach
    void setUp() {
        service = new DashboardWidgetService(widgetMapper);
    }

    @Test
    @DisplayName("isEnabled → 无记录默认显示")
    void isEnabled_noRecord_shouldDefaultTrue() {
        when(widgetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertTrue(service.isEnabled("admin", "cpuChart"));
    }

    @Test
    @DisplayName("isEnabled → enabled=0 返回 false")
    void isEnabled_disabled_shouldReturnFalse() {
        DashboardWidget w = new DashboardWidget();
        w.setEnabled(0);
        when(widgetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(w);
        assertFalse(service.isEnabled("admin", "cpuChart"));
    }

    @Test
    @DisplayName("isEnabled → enabled=1 返回 true")
    void isEnabled_enabled_shouldReturnTrue() {
        DashboardWidget w = new DashboardWidget();
        w.setEnabled(1);
        when(widgetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(w);
        assertTrue(service.isEnabled("admin", "cpuChart"));
    }

    @Test
    @DisplayName("update → 不存在则插入")
    void update_notExists_shouldInsert() {
        when(widgetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(widgetMapper.insert(any(DashboardWidget.class))).thenAnswer(inv -> {
            inv.getArgument(0, DashboardWidget.class).setId(1L);
            return 1;
        });

        DashboardWidget result = service.update("admin", "cpuChart", false);
        assertFalse(result.getEnabled() == 1);
        verify(widgetMapper).insert(any(DashboardWidget.class));
    }

    @Test
    @DisplayName("update → 已存在则更新")
    void update_exists_shouldUpdate() {
        DashboardWidget existing = new DashboardWidget();
        existing.setId(1L);
        existing.setEnabled(1);
        when(widgetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        DashboardWidget result = service.update("admin", "cpuChart", false);
        assertEquals(0, result.getEnabled());
        verify(widgetMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("prefs → 返回用户偏好列表")
    void prefs_shouldReturnList() {
        when(widgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        List<DashboardWidget> result = service.prefs("admin");
        assertNotNull(result);
    }
}