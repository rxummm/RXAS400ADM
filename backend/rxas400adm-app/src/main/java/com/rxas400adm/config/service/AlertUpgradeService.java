package com.rxas400adm.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rxas400adm.monitor.alert.AlertEvent;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.service.INotificationService;
import com.rxas400adm.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 告警未处理自动升级服务（E1）。
 * <p>
 * 定期检查超过阈值时间仍未处理的告警事件（status = OPEN 且未发送过升级通知），
 * 按 {@code notify-role} 配置的角色解析目标用户，逐用户发送定向站内通知；
 * 发送成功后回写 upgrade_notified 标记去重，避免同一告警重复轰炸。
 *
 * <p>2026-08-24 修复：① 定向通知（原实现发全局广播且循环重复）；
 * ② 调度间隔单位错误（分钟值被当毫秒拼接）；③ 去重标记缺失导致无限重发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertUpgradeService {

    /** 升级通知的站内信类型（前端通知中心按 type 过滤图标/文案） */
    public static final String NOTIFY_TYPE = "ALERT_UPGRADE";

    private final AlertEventMapper alertEventMapper;
    private final SysUserMapper sysUserMapper;
    private final INotificationService notificationService;
    private final AlertUpgradeProperties props;

    /** 【第六章·P3】notify-role 运行时来源：rx_config 键 alert.upgrade.notify-role，缺省回落 yml 值 */
    public static final String KEY_NOTIFY_ROLE = "alert.upgrade.notify-role";

    private final SysConfigService sysConfigService;

    /** 目标角色（rx_config 覆盖 yml；每轮读取一次，频率低无需缓存） */
    private String notifyRole() {
        return sysConfigService.get(KEY_NOTIFY_ROLE, props.getNotifyRole());
    }

    /** C5/C7：执行中标志——调度 tick 与手动触发并发重入时，后到者直接跳过，防止重复通知轰炸 */
    private final AtomicBoolean upgrading = new AtomicBoolean(false);

    /**
     * 定时检查未处理告警并升级通知。仅在功能开启时执行；
     * 间隔由 check-interval-ms 控制，默认 5 分钟（300000ms）。
     */
    @Scheduled(fixedDelayString = "${rxas400.alert.upgrade.check-interval-ms:300000}")
    public void checkAndUpgradeAlerts() {
        if (!props.isEnabled()) {
            return;
        }
        if (!upgrading.compareAndSet(false, true)) {
            log.info("上一轮告警升级仍在执行，本轮跳过");
            return;
        }
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(props.getUpgradeAfterMinutes());
            List<AlertEvent> unprocessedAlerts = alertEventMapper.selectList(
                    new LambdaQueryWrapper<AlertEvent>()
                            .eq(AlertEvent::getStatus, "OPEN")
                            .eq(AlertEvent::getUpgradeNotified, 0)
                            .le(AlertEvent::getCreatedTime, threshold)
                            .orderByAsc(AlertEvent::getCreatedTime));
            if (unprocessedAlerts.isEmpty()) {
                return;
            }

            // C7/P7：先原子抢占置位（单条 UPDATE ... WHERE id IN (...) AND upgrade_notified=0），
            // 抢到几条发几条——并发重入/多实例场景同一告警只会被一方处理；
            // 发送中途异常时已抢占的不再重发（at-most-once），未抢占的留待下轮
            List<Long> candidateIds = unprocessedAlerts.stream().map(AlertEvent::getId).toList();
            alertEventMapper.update(null, new LambdaUpdateWrapper<AlertEvent>()
                    .in(AlertEvent::getId, candidateIds)
                    .eq(AlertEvent::getUpgradeNotified, 0)
                    .set(AlertEvent::getUpgradeNotified, 1));

            List<AlertEvent> claimed = alertEventMapper.selectList(
                    new LambdaQueryWrapper<AlertEvent>().in(AlertEvent::getId, candidateIds))
                    .stream().filter(a -> a.getUpgradeNotified() != null && a.getUpgradeNotified() == 1)
                    .toList();
            if (claimed.isEmpty()) {
                return;
            }

            List<SysUser> notifyUsers = sysUserMapper.selectUsersByRoleCode(notifyRole());
            if (notifyUsers.isEmpty()) {
                log.warn("未找到角色 {} 的用户，跳过升级通知", notifyRole());
                return;
            }

            log.info("发现 {} 条超时未处理告警，向角色 {} 的 {} 个用户发送升级通知",
                    claimed.size(), notifyRole(), notifyUsers.size());
            dispatchNotifications(claimed, notifyUsers);
            log.info("告警升级通知完成：{} 条告警已标记 notified", claimed.size());
        } catch (Exception e) {
            log.error("告警升级检查失败", e);
        } finally {
            upgrading.set(false);
        }
    }

    /** 逐用户定向发送站内通知（标题 + 摘要正文） */
    private void dispatchNotifications(List<AlertEvent> alerts, List<SysUser> users) {
        String title = "Alert Escalation";
        String content = String.format(
                "%d alert(s) unhandled for over %d minutes, please address promptly:%n%s",
                alerts.size(), props.getUpgradeAfterMinutes(), formatAlertSummary(alerts));
        for (SysUser user : users) {
            notificationService.send(user.getUsername(), NOTIFY_TYPE, title, content);
        }
    }

    /** 格式化告警摘要信息（最多展示 5 条，防止通知过长） */
    private String formatAlertSummary(List<AlertEvent> alerts) {
        StringBuilder sb = new StringBuilder();
        int displayCount = Math.min(alerts.size(), 5);
        for (int i = 0; i < displayCount; i++) {
            AlertEvent alert = alerts.get(i);
            sb.append(String.format("- [%s] %s (%s)%n",
                    alert.getLevel(), alert.getMessage(), alert.getCreatedTime()));
        }
        if (alerts.size() > displayCount) {
            sb.append(String.format("... and %d more alert(s)%n", alerts.size() - displayCount));
        }
        return sb.toString();
    }
}
