package com.rxas400adm.monitor.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.monitor.alert.AlertRule;
import com.rxas400adm.monitor.dto.AlertRuleDTO;
import com.rxas400adm.monitor.mapper.AlertRuleMapper;
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
class AlertRuleServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, AlertRule.class);
    }

    @Mock
    private AlertRuleMapper ruleMapper;

    private AlertRuleService service;

    @BeforeEach
    void setUp() {
        service = new AlertRuleService(ruleMapper);
    }

    private AlertRule sampleRule(Long id) {
        AlertRule rule = new AlertRule();
        rule.setId(id);
        rule.setMetricName("CPU_USAGE");
        rule.setThreshold(90.0);
        rule.setLevel("WARNING");
        rule.setEnabled(true);
        rule.setChannel("ALL");
        return rule;
    }

    @Test
    @DisplayName("list → 按 metricName + threshold 排序")
    void list_shouldReturn() {
        when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sampleRule(1L)));
        List<AlertRule> result = service.list();
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("create → 新建规则，channel 默认 ALL，enabled 默认 true")
    void create_shouldDefaultChannelAndEnabled() {
        when(ruleMapper.insert(any(AlertRule.class))).thenAnswer(inv -> {
            inv.getArgument(0, AlertRule.class).setId(1L);
            return 1;
        });

        AlertRuleDTO dto = new AlertRuleDTO("CPU_USAGE", ">", 90.0, null, "WARNING", null, null, null, null);

        AlertRule created = service.create(dto);
        assertEquals("CPU_USAGE", created.getMetricName());
        assertEquals("ALL", created.getChannel());
        assertTrue(created.getEnabled());
    }

    @Test
    @DisplayName("create → channel 显式设置时不覆盖")
    void create_explicitChannel_shouldKeep() {
        when(ruleMapper.insert(any(AlertRule.class))).thenAnswer(inv -> {
            inv.getArgument(0, AlertRule.class).setId(1L);
            return 1;
        });

        AlertRuleDTO dto = new AlertRuleDTO("CPU", ">", 90.0, null, "WARNING", null, "EMAIL", null, null);

        AlertRule created = service.create(dto);
        assertEquals("EMAIL", created.getChannel());
    }

    @Test
    @DisplayName("update → 规则不存在抛异常")
    void update_notFound_shouldThrow() {
        when(ruleMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.update(999L, new AlertRuleDTO("CPU", ">", 90.0, null, "WARNING", null, null, null, null)));
    }

    @Test
    @DisplayName("update → 规则存在则更新")
    void update_found_shouldUpdate() {
        AlertRule existing = sampleRule(1L);
        when(ruleMapper.selectById(1L)).thenReturn(existing);

        AlertRuleDTO dto = new AlertRuleDTO("MEM_USAGE", ">", 85.0, null, "CRITICAL", null, null, null, null);

        AlertRule result = service.update(1L, dto);
        assertEquals("MEM_USAGE", result.getMetricName());
        verify(ruleMapper).updateById(result);
    }

    @Test
    @DisplayName("delete → 规则不存在抛异常")
    void delete_notFound_shouldThrow() {
        when(ruleMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.delete(999L));
    }

    @Test
    @DisplayName("delete → 规则存在则删除")
    void delete_found_shouldDelete() {
        when(ruleMapper.selectById(1L)).thenReturn(sampleRule(1L));
        service.delete(1L);
        verify(ruleMapper).deleteById(1L);
    }

    @Test
    @DisplayName("toggle → 切换启用状态")
    void toggle_shouldChangeEnabled() {
        AlertRule rule = sampleRule(1L);
        when(ruleMapper.selectById(1L)).thenReturn(rule);

        AlertRule result = service.toggle(1L, false);
        assertFalse(result.getEnabled());
        verify(ruleMapper).updateById(result);
    }

    @Test
    @DisplayName("toggle → 规则不存在抛异常")
    void toggle_notFound_shouldThrow() {
        when(ruleMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.toggle(999L, true));
    }
}
