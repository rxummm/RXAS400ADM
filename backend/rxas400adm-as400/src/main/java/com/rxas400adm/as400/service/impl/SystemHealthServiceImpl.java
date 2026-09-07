package com.rxas400adm.as400.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.entity.BackupStatus;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.entity.SystemValueCompliance;
import com.rxas400adm.as400.mapper.BackupStatusMapper;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.as400.mapper.SystemValueComplianceMapper;
import com.rxas400adm.as400.service.ISystemHealthService;
import com.rxas400adm.as400.vo.SecurityAuditSummaryVO;
import com.rxas400adm.as400.vo.SystemHealthOverviewVO;
import com.rxas400adm.as400.vo.UserPermissionMatrixVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A1/A2/A3 系统健康与安全服务实现
 * 聚合多服务器指标，提供系统级综合视图
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemHealthServiceImpl implements ISystemHealthService {

    private final IbmiSystemMapper systemMapper;
    private final BackupStatusMapper backupStatusMapper;
    private final SystemValueComplianceMapper complianceMapper;

    @Override
    public SystemHealthOverviewVO getOverview() {
        SystemHealthOverviewVO vo = new SystemHealthOverviewVO();

        // 服务器统计
        List<IbmiSystem> systems = systemMapper.selectList(null);
        vo.setTotalServers((long) systems.size());
        vo.setOnlineServers(systems.stream().filter(s -> "ONLINE".equals(s.getStatus())).count());
        vo.setOfflineServers(vo.getTotalServers() - vo.getOnlineServers());

        // CPU/内存/磁盘（mock 数据，生产环境通过 AS400Client 采集）
        vo.setAvgCpuUsage(45.2);
        vo.setAvgMemoryUsage(62.8);
        vo.setAvgDiskUsage(38.5);
        vo.setActiveJobs(128L);
        vo.setAlertCount(5L);
        vo.setCriticalAlertCount(1L);

        // 今日备份统计
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<BackupStatus> backupWrapper = new LambdaQueryWrapper<BackupStatus>()
                .ge(BackupStatus::getStartTime, todayStart);
        List<BackupStatus> todayBackups = backupStatusMapper.selectList(backupWrapper);
            vo.setTodayBackupSuccess(todayBackups.stream().filter(b -> "SUCCESS".equals(b.getStatus())).count());
            vo.setTodayBackupFailed(todayBackups.stream().filter(b -> "FAILED".equals(b.getStatus())).count());

        // 合规通过率
        long totalChecks = complianceMapper.selectCount(null);
        long passedChecks = complianceMapper.selectCount(new LambdaQueryWrapper<SystemValueCompliance>()
                .eq(SystemValueCompliance::getComplianceStatus, "PASS"));
        vo.setComplianceRate(totalChecks > 0 ? (double) passedChecks / totalChecks * 100 : 100.0);

        return vo;
    }

    @Override
    public List<SecurityAuditSummaryVO> getSecurityAuditSummary() {
        List<SecurityAuditSummaryVO> result = new ArrayList<>();
        List<IbmiSystem> systems = systemMapper.selectList(null);

        for (IbmiSystem system : systems) {
            SecurityAuditSummaryVO vo = new SecurityAuditSummaryVO();
            vo.setServerId(system.getId());
            vo.setServerName(system.getName());
            // mock 数据，生产环境通过 AS400Client 查询安全日志
            vo.setTotalLogins(1250L);
            vo.setSuccessfulLogins(1180L);
            vo.setFailedLogins(70L);
            vo.setUniqueUsers(15L);
            vo.setUniqueIps(8L);
            vo.setPermissionChanges(12L);
            vo.setHighRiskOperations(3L);
            vo.setLastLoginTime(LocalDateTime.now().minusHours(2));
            vo.setLastHighRiskTime(LocalDateTime.now().minusDays(1));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<UserPermissionMatrixVO> getUserPermissionMatrix() {
        List<UserPermissionMatrixVO> result = new ArrayList<>();
        // 用户权限矩阵数据来自系统管理模块，此处返回空列表作为占位
        // 生产环境需通过跨模块调用或 API 聚合获取
        return result;
    }
}
