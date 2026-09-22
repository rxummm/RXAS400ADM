package com.rxas400adm.as400.scheduler;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.UserProfileLog;
import com.rxas400adm.as400.mapper.UserProfileLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户Profile定时任务：
 * - 扫描90天未登录用户并标记待删除
 * - 扫描离职用户（EMPINFOPF表中TERMDT有值）并标记待删除
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileCleanupScheduler {

    private final AS400ClientProvider clientProvider;
    private final UserProfileLogMapper userProfileLogMapper;

    private static final int INACTIVE_DAYS_THRESHOLD = 90;

    /**
     * 每24小时执行一次，扫描90天未登录的IBM i用户
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scanInactiveUsers() {
        log.info("[用户清理] 开始扫描90天未登录用户...");
        
        try {
            AS400Client client = clientProvider.current();
            
            // 查询90天未登录的用户
            String sql = """
                SELECT USER_NAME, LAST_USED_DATE
                FROM QSYS2.USER_INFO
                WHERE STATUS = '*ENABLED'
                  AND LAST_USED_DATE < CURRENT_DATE - %d DAYS
                  AND USER_NAME NOT IN ('QSECOFR', 'QSYSCTL', 'QPGMR')
                ORDER BY LAST_USED_DATE
                """.formatted(INACTIVE_DAYS_THRESHOLD);
            
            List<Map<String, Object>> results = client.queryList(sql);
            
            if (results.isEmpty()) {
                log.info("[用户清理] 未发现90天未登录的用户");
                return;
            }
            
            List<String> inactiveUsers = new ArrayList<>();
            for (Map<String, Object> row : results) {
                String userName = getStringValue(row, "USER_NAME");
                String lastUsed = getStringValue(row, "LAST_USED_DATE");
                inactiveUsers.add(userName);
                
                // 记录删除日志
                saveCleanupLog(userName, "INACTIVE_90D", "超过" + INACTIVE_DAYS_THRESHOLD + "天未登录");
                log.info("[用户清理] 标记待删除: {}, 最后使用: {}", userName, lastUsed);
            }
            
            log.info("[用户清理] 共发现 {} 个90天未登录的用户，已标记待删除", inactiveUsers.size());
            
        } catch (Exception e) {
            log.error("[用户清理] 扫描90天未登录用户失败: {}", e.getMessage());
        }
    }

    /**
     * 每24小时执行一次，扫描EMPINFOPF表中已离职用户
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void scanResignedUsers() {
        log.info("[用户清理] 开始扫描离职用户（EMPINFOPF）...");
        
        try {
            AS400Client client = clientProvider.current();
            
            // 查询离职用户（TERMDT有值的用户）
            // 注意：实际表名和字段名需要根据IBM i系统实际情况调整
            String sql = """
                SELECT USER_NAME, TERM_DATE, TERM_REASON
                FROM QSYS2.SYSAUTH_USERS
                WHERE TERM_DATE IS NOT NULL
                  AND TERM_DATE < CURRENT_DATE
                ORDER BY TERM_DATE
                """;
            
            List<Map<String, Object>> results = client.queryList(sql);
            
            if (results.isEmpty()) {
                log.info("[用户清理] 未发现已离职的用户");
                return;
            }
            
            for (Map<String, Object> row : results) {
                String userName = getStringValue(row, "USER_NAME");
                String termDate = getStringValue(row, "TERM_DATE");
                String termReason = getStringValue(row, "TERM_REASON");
                
                // 记录删除日志
                saveCleanupLog(userName, "RESIGNED", 
                    "离职日期: " + termDate + (StringUtils.hasText(termReason) ? ", 原因: " + termReason : ""));
                log.info("[用户清理] 标记离职用户待删除: {}, 离职日期: {}", userName, termDate);
            }
            
            log.info("[用户清理] 共发现 {} 个已离职用户，已标记待删除", results.size());
            
        } catch (Exception e) {
            log.error("[用户清理] 扫描离职用户失败: {}", e.getMessage());
        }
    }

    /**
     * 每周一凌晨3点执行，批量清理标记为待删除的90天未登录用户
     */
    @Scheduled(cron = "0 0 3 * * MON")
    public void cleanupInactiveUsers() {
        log.info("[用户清理] 开始批量清理90天未登录用户...");
        
        try {
            AS400Client client = clientProvider.current();
            
            // 查询待清理的用户（从日志中获取）
            String sql = """
                SELECT USER_NAME
                FROM QSYS2.USER_INFO
                WHERE STATUS = '*ENABLED'
                  AND LAST_USED_DATE < CURRENT_DATE - %d DAYS
                  AND USER_NAME NOT IN ('QSECOFR', 'QSYSCTL', 'QPGMR')
                """.formatted(INACTIVE_DAYS_THRESHOLD);
            
            List<Map<String, Object>> results = client.queryList(sql);
            
            if (results.isEmpty()) {
                log.info("[用户清理] 无待清理的90天未登录用户");
                return;
            }
            
            int deletedCount = 0;
            for (Map<String, Object> row : results) {
                String userName = getStringValue(row, "USER_NAME");
                try {
                    // 执行删除
                    String command = "DLTUSRPRF USRPRF(" + userName.toUpperCase() + ")";
                    client.execute(command);
                    deletedCount++;
                    log.info("[用户清理] 已删除90天未登录用户: {}", userName);
                } catch (Exception e) {
                    log.error("[用户清理] 删除用户 {} 失败: {}", userName, e.getMessage());
                }
            }
            
            log.info("[用户清理] 批量清理完成，共删除 {} 个90天未登录用户", deletedCount);
            
        } catch (Exception e) {
            log.error("[用户清理] 批量清理90天未登录用户失败: {}", e.getMessage());
        }
    }

    private void saveCleanupLog(String userName, String type, String reason) {
        UserProfileLog logEntry = new UserProfileLog();
        logEntry.setUserName(userName);
        logEntry.setAction("DELETE");
        logEntry.setOperator("SYSTEM_SCHEDULER");
        logEntry.setDetail("定时任务标记待删除");
        logEntry.setDeleteReason(reason);
        logEntry.setDeletionType(type);
        logEntry.setCreatedTime(LocalDateTime.now());
        userProfileLogMapper.insert(logEntry);
    }

    private String getStringValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }
}
