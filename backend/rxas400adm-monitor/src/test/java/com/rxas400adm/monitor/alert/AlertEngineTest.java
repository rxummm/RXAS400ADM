package com.rxas400adm.monitor.alert;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.rxas400adm.monitor.domain.Metric;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import com.rxas400adm.monitor.mapper.AlertRuleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class AlertEngineTest {

    private final AlertRuleMapper ruleMapper = mock(AlertRuleMapper.class);
    private final AlertEventMapper eventMapper = mock(AlertEventMapper.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    @Test
    void metricOverThreshold_shouldCreateAlertEvent() {
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        rule.setMetricName("CPU");
        rule.setOperator(">");
        rule.setThreshold(90.0);
        rule.setLevel("CRITICAL");
        rule.setEnabled(true);
        when(ruleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rule));

        Metric metric = Metric.builder()
                .instanceId(1L)
                .metricName("CPU")
                .metricValue(95.5)
                .build();
        AlertEngine engine = new AlertEngine(ruleMapper, eventMapper, eventPublisher);
        engine.check(metric);

        verify(eventMapper).insert(org.mockito.ArgumentCaptor.forClass(AlertEvent.class).capture());
        verify(eventPublisher).publishEvent(
                org.mockito.ArgumentMatchers.any(com.rxas400adm.common.event.AlertRaisedEvent.class));
    }

    @Test
    void metricWithinThreshold_shouldNotCreateAlert() {
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        rule.setMetricName("CPU");
        rule.setOperator(">");
        rule.setThreshold(90.0);
        rule.setLevel("CRITICAL");
        rule.setEnabled(true);
        when(ruleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rule));

        Metric metric = Metric.builder()
                .instanceId(1L)
                .metricName("CPU")
                .metricValue(50.0)
                .build();
        AlertEngine engine = new AlertEngine(ruleMapper, eventMapper, eventPublisher);
        engine.check(metric);

        verify(eventMapper, never()).insert(any(AlertEvent.class));
        verify(eventPublisher, never()).publishEvent(
                org.mockito.ArgumentMatchers.any(com.rxas400adm.common.event.AlertRaisedEvent.class));
    }

    @Test
    void insertedEvent_shouldCaptureLevelAndMessage() {
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        rule.setMetricName("CPU");
        rule.setOperator(">");
        rule.setThreshold(90.0);
        rule.setLevel("CRITICAL");
        rule.setEnabled(true);
        when(ruleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rule));

        AlertEngine engine = new AlertEngine(ruleMapper, eventMapper, eventPublisher);
        engine.check(Metric.builder().instanceId(7L).metricName("CPU").metricValue(99.0).build());

        org.mockito.ArgumentCaptor<AlertEvent> captor =
                org.mockito.ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventMapper).insert(captor.capture());
        AlertEvent event = captor.getValue();
        assertEquals(7L, event.getInstanceId());
        assertEquals("CRITICAL", event.getLevel());
        assertEquals("OPEN", event.getStatus());
        assertEquals(1L, event.getRuleId());
    }

    /**
     * 持续超阈值路径（P2 指标规则测试）：durationSeconds > 0 时，
     * 首次越界不告警，持续满 N 秒后才 OPEN，且 OPEN 期间不重复告警。
     */
    @Test
    void sustainedBreach_shouldOpenOnlyAfterDurationElapsed() {
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        rule.setMetricName("CPU");
        rule.setOperator(">");
        rule.setThreshold(90.0);
        rule.setDurationSeconds(300);
        rule.setLevel("CRITICAL");
        rule.setEnabled(true);
        when(ruleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rule));

        TestEngine engine = new TestEngine(ruleMapper, eventMapper, eventPublisher);
        Metric hot = Metric.builder().instanceId(1L).metricName("CPU").metricValue(95.5).build();

        // t0 越界：未持续满 300s，不告警
        engine.check(hot);
        verify(eventMapper, never()).insert(any(AlertEvent.class));

        // t0+60：仍未满，不告警
        engine.advanceSeconds(60);
        engine.check(hot);
        verify(eventMapper, never()).insert(any(AlertEvent.class));

        // t0+301：持续满 300s → 恰好一次 OPEN
        engine.advanceSeconds(241);
        engine.check(hot);
        org.mockito.ArgumentCaptor<AlertEvent> captor =
                org.mockito.ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventMapper, times(1)).insert(captor.capture());
        assertEquals("OPEN", captor.getValue().getStatus());

        // OPEN 期间持续越界：去重，不重复告警
        engine.advanceSeconds(60);
        engine.check(hot);
        verify(eventMapper, times(1)).insert(any(AlertEvent.class));
    }

    /**
     * 抖动恢复路径：持续时长未达标就恢复（抖动）不应产生任何告警（OPEN/CLOSED 都没有），
     * 且恢复后再次越界会重新计时；只有真正 OPEN 后的恢复才发 CLOSED。
     */
    @Test
    void jitterRecovery_beforeDuration_shouldNotFalseAlarm() {
        AlertRule rule = new AlertRule();
        rule.setId(1L);
        rule.setMetricName("CPU");
        rule.setOperator(">");
        rule.setThreshold(90.0);
        rule.setDurationSeconds(300);
        rule.setLevel("WARNING");
        rule.setEnabled(true);
        when(ruleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rule));

        TestEngine engine = new TestEngine(ruleMapper, eventMapper, eventPublisher);
        Metric hot = Metric.builder().instanceId(1L).metricName("CPU").metricValue(95.0).build();
        Metric cool = Metric.builder().instanceId(1L).metricName("CPU").metricValue(50.0).build();

        // t0 越界（未满 300s），t0+10 恢复 → 抖动，全程零事件
        engine.check(hot);
        engine.advanceSeconds(10);
        engine.check(cool);
        verify(eventMapper, never()).insert(any(AlertEvent.class));
        verify(eventPublisher, never()).publishEvent(
                org.mockito.ArgumentMatchers.any(com.rxas400adm.common.event.AlertRaisedEvent.class));

        // 恢复后再次越界：重新计时，t0+11 起的 300s 内仍不告警
        engine.advanceSeconds(1);
        engine.check(hot);
        engine.advanceSeconds(299);
        engine.check(hot);
        verify(eventMapper, never()).insert(any(AlertEvent.class));

        // 满 300s → 恰好一次 OPEN
        engine.advanceSeconds(1);
        engine.check(hot);
        org.mockito.ArgumentCaptor<AlertEvent> openCaptor =
                org.mockito.ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventMapper, times(1)).insert(openCaptor.capture());
        assertEquals("OPEN", openCaptor.getValue().getStatus());

        // OPEN 之后的恢复才发 CLOSED（一次）
        engine.advanceSeconds(1);
        engine.check(cool);
        org.mockito.ArgumentCaptor<AlertEvent> closedCaptor =
                org.mockito.ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventMapper, times(2)).insert(closedCaptor.capture());
        assertEquals("CLOSED", closedCaptor.getAllValues().get(1).getStatus());

        // 未 OPEN 的抖动从未留下残留状态：再次越界仍从零计时
        engine.advanceSeconds(1);
        engine.check(hot);
        verify(eventMapper, times(2)).insert(any(AlertEvent.class));
    }

    /** 测试用子类：可控时间源，用于推进 durationSeconds 持续窗口 */
    private static final class TestEngine extends AlertEngine {
        private LocalDateTime current;

        TestEngine(AlertRuleMapper ruleMapper, AlertEventMapper eventMapper,
                   ApplicationEventPublisher eventPublisher) {
            super(ruleMapper, eventMapper, eventPublisher);
            this.current = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        }

        void advanceSeconds(long seconds) {
            current = current.plusSeconds(seconds);
        }

        @Override
        LocalDateTime now() {
            return current;
        }
    }
}