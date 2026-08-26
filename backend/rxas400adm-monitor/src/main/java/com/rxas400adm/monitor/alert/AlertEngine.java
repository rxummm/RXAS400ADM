package com.rxas400adm.monitor.alert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rxas400adm.common.event.AlertRaisedEvent;
import com.rxas400adm.monitor.domain.Metric;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import com.rxas400adm.monitor.mapper.AlertRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 告警引擎：Metric → Rule Engine → Alert → Notification（Email/Teams/Slack/Webhook）。
 *
 * 行为约定（P1-10，durationSeconds 真正生效）：
 * - durationSeconds <= 0：首次命中阈值立即告警（OPEN）；
 * - durationSeconds > 0：持续超阈值 N 秒后才告警（短暂抖动不误报），
 *   未达标前恢复（回到阈值内）则不产生任何告警；
 * - 同一条规则同一服务器 OPEN 期间不重复发告警（去重防刷屏）；
 * - 恢复（回到阈值内）时发一次 CLOSED 并释放状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEngine {

    /** 单条规则的持续越界状态：ruleId:instanceId → 首次越界时间 + 是否已告警（C6：CAS 原子置位） */
    private static final class BreachState {
        volatile LocalDateTime since;
        private final AtomicBoolean alerted = new AtomicBoolean(false);

        BreachState(LocalDateTime since) {
            this.since = since;
        }

        /** C6：原子「检查并置位」——并发评估同一 rule:instance 时只有一个线程发 OPEN */
        boolean markAlertedIfFirst() {
            return alerted.compareAndSet(false, true);
        }

        boolean hasAlerted() {
            return alerted.get();
        }

        /** 未达持续时长时回退标记，继续观察 */
        void resetAlerted() {
            alerted.set(false);
        }
    }

    private final Map<String, BreachState> breaches = new ConcurrentHashMap<>();

    /**
     * P2：规则缓存（metricName → rules，TTL 45s）——告警每 tick × 每指标 selectList 是最高频重复查询。
     * 规则增删改最长 45s 后生效（监控场景可接受）；如需即时生效可在规则写服务处注入本组件做失效。
     */
    private final Cache<String, List<AlertRule>> ruleCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(45))
            .maximumSize(500)
            .build();

    private final AlertRuleMapper ruleMapper;
    private final AlertEventMapper eventMapper;
    private final ApplicationEventPublisher eventPublisher;

    public void check(Metric metric) {
        List<AlertRule> rules = ruleCache.get(metric.getMetricName(), name ->
                ruleMapper.selectList(new LambdaQueryWrapper<AlertRule>()
                        .eq(AlertRule::getMetricName, name)));
        for (AlertRule rule : rules) {
            if (rule.getServerId() != null && !rule.getServerId().equals(metric.getInstanceId())) {
                continue;
            }
            boolean matched = rule.match(metric.getMetricValue());
            String key = rule.getId() + ":" + metric.getInstanceId();

            if (matched) {
                onMatch(metric, rule, key);
            } else {
                onRecover(metric, rule, key);
            }
        }
    }

    private void onMatch(Metric metric, AlertRule rule, String key) {
        BreachState state = breaches.computeIfAbsent(key, k -> new BreachState(now()));
        if (!state.markAlertedIfFirst()) {
            // C6：另一线程已抢先置位（已 OPEN）：持续超阈值不重复告警（天然去重）
            // 注意：未达持续时长时也会先占住标记，恢复走 onRecover 释放，行为与原实现一致
            return;
        }
        long duration = rule.getDurationSeconds() == null ? 0L : rule.getDurationSeconds();
        boolean sustained = duration <= 0
                || Duration.between(state.since, now()).getSeconds() >= duration;
        if (sustained) {
            createEvent(metric, rule, "OPEN");
            return;
        }
        // 未达持续时长：回退标记，继续观察（否则抖动期间永久吞掉后续真实告警）
        state.resetAlerted();
    }

    private void onRecover(Metric metric, AlertRule rule, String key) {
        BreachState state = breaches.remove(key);
        // 仅当已告警过才发 CLOSED；持续时长未达标就恢复的抖动不产生任何告警
        if (state != null && state.hasAlerted()) {
            createEvent(metric, rule, "CLOSED");
        }
    }

    private void createEvent(Metric metric, AlertRule rule, String status) {
        AlertEvent event = new AlertEvent();
        event.setInstanceId(metric.getInstanceId());
        event.setRuleId(rule.getId());
        event.setLevel(rule.getLevel());
        event.setMessage(metric.getMetricName() + " " + rule.getOperator() + " "
                + rule.getThreshold() + ", current " + metric.getMetricValue());
        event.setStatus(status);
        event.setCreatedTime(now());
        eventMapper.insert(event);
        log.warn("Alert {} [{}] {}", status, rule.getLevel(), event.getMessage());
        eventPublisher.publishEvent(new AlertRaisedEvent(rule.getLevel(), "Monitor Alert",
                event.getMessage(), metric.getInstanceId(), rule.getChannel()));
    }

    /** 时间源（默认系统时钟；测试子类可覆写以推进 durationSeconds 持续窗口） */
    LocalDateTime now() {
        return LocalDateTime.now();
    }
}