package com.rxas400adm.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.event.AlertRaisedEvent;
import com.rxas400adm.monitor.alert.AlertEvent;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警未处理自动升级服务。
 * <p>
 * 定期检查未处理的告警事件（status = OPEN），若超过配置时间未处理，
 * 自动发送站内通知给指定角色的用户。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertUpgradeService {

    private final AlertEventMapper alertEventMapper;
    private final SysUserMapper sysUserMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${rxas400.alert.upgrade.enabled:false}")
    private boolean upgradeEnabled;

    @Value("${rxas400.alert.upgrade.check-interval-minutes:5}")
    private int checkIntervalMinutes;

    @Value("${rxas400.alert.upgrade.upgrade-after-minutes:30}")
    private int upgradeAfterMinutes;

    @Value("${rxas400.alert.upgrade.notify-role:ADMIN}")
    private String notifyRole;

    /**
     * 定时检查未处理告警并升级通知。
     * 仅在功能开启时执行。
     */
    @Scheduled(fixedDelayString = "${rxas400.alert.upgrade.check-interval-minutes:5}000")
    public void checkAndUpgradeAlerts() {
        if (!upgradeEnabled) {
            return;
        }

        try {
            // 查询超过配置时间仍未处理的告警
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(upgradeAfterMinutes);
            List<AlertEvent> unprocessedAlerts = alertEventMapper.selectList(
                    new LambdaQueryWrapper<AlertEvent>()
                            .eq(AlertEvent::getStatus, "OPEN")
                            .le(AlertEvent::getCreatedTime, threshold)
                            .orderByAsc(AlertEvent::getCreatedTime)
            );

            if (unprocessedAlerts.isEmpty()) {
                return;
            }

            log.info("发现 {} 条未处理告警，准备升级通知", unprocessedAlerts.size());

            // 获取需要通知的用户（指定角色）
            List<SysUser> notifyUsers = sysUserMapper.selectUsersByRoleCode(notifyRole);
            if (notifyUsers.isEmpty()) {
                log.warn("未找到角色 {} 的用户，跳过升级通知", notifyRole);
                return;
            }

            // 构建升级通知内容
            String title = "告警升级通知";
            String content = String.format(
                    "有 %d 条告警超过 %d 分钟未处理，请及时处理：%n%s",
                    unprocessedAlerts.size(),
                    upgradeAfterMinutes,
                    formatAlertSummary(unprocessedAlerts)
            );

            // 发送站内通知给指定角色用户
            for (SysUser user : notifyUsers) {
                eventPublisher.publishEvent(new AlertRaisedEvent(
                        "CRITICAL",
                        "AlertUpgradeService",
                        content,
                        null,
                        "INAPP"
                ));
                log.debug("已发送升级通知给用户: {}", user.getUsername());
            }

            log.info("告警升级通知发送完成，共 {} 条告警，{} 个用户",
                    unprocessedAlerts.size(), notifyUsers.size());

        } catch (Exception e) {
            log.error("告警升级检查失败", e);
        }
    }

    /**
     * 格式化告警摘要信息
     */
    private String formatAlertSummary(List<AlertEvent> alerts) {
        StringBuilder sb = new StringBuilder();
        int displayCount = Math.min(alerts.size(), 5); // 最多显示5条
        for (int i = 0; i < displayCount; i++) {
            AlertEvent alert = alerts.get(i);
            sb.append(String.format("- [%s] %s (%s)%n",
                    alert.getLevel(),
                    alert.getMessage(),
                    alert.getCreatedTime()));
        }
        if (alerts.size() > displayCount) {
            sb.append(String.format("... 还有 %d 条告警%n", alerts.size() - displayCount));
        }
        return sb.toString();
    }

    /**
     * 手动触发告警升级检查（供管理员使用）
     */
    public void triggerUpgradeCheck() {
        log.info("手动触发告警升级检查");
        checkAndUpgradeAlerts();
    }
}
