package com.rxas400adm.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.mapper.IbmiSystemMapper;
import com.rxas400adm.config.vo.HealthReportVO;
import com.rxas400adm.monitor.alert.AlertEvent;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 平台健康巡检数据服务（R1 分层清零：Controller 不碰 Mapper/QueryWrapper）。
 * <p>
 * 【R2】HealthController.report() 的全部编排逻辑下沉至此（reportWithServers），
 * Controller 只保留参数接收 + 委托 + ApiResponse 包装。
 * 【P3】多服务器探测改为 healthProbePool 并行 fan-out，结果入 30s Caffeine 缓存
 * （key=serverId），Actuator 的 {@link As400HealthIndicator} 与 REST 巡检共用同一份缓存快照。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

    /** 【P3】单台探测结果缓存 TTL：30s 内重复巡检/Actuator 检查不再逐台打 IBM i */
    private static final Duration PROBE_CACHE_TTL = Duration.ofSeconds(30);
    /** 【P3】缓存上限（与服务器注册量级匹配，防失控） */
    private static final long PROBE_CACHE_MAX = 100;

    private final IbmiSystemMapper systemMapper;
    private final AlertEventMapper alertEventMapper;
    private final AS400ClientProvider clientProvider;
    private final Scheduler scheduler;
    /** 与 MonitorService.compare 共用的探测池（bean 名与字段名一致，按名注入） */
    private final Executor healthProbePool;

    /**
     * 【P3】单台服务器探测结果缓存：ServerHealthVO 为不可变 record，命中可直接返回快照；
     * key=serverId，TTL 30s，maximumSize=100。
     */
    private final Cache<Long, HealthReportVO.ServerHealthVO> serverProbeCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(PROBE_CACHE_TTL)
                    .maximumSize(PROBE_CACHE_MAX)
                    .build();

    /** DB 连通性轻量探测 */
    public boolean probeDatabase() {
        try {
            systemMapper.selectCount(null);
            return true;
        } catch (Exception e) {
            // 【E16】DB 探测失败日志 debug 升 warn 并带异常对象（原仅 getMessage 丢堆栈）
            log.warn("DB probe failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /** 按 sortOrder 升序的全部服务器（健康巡检逐台连接测试用） */
    public List<IbmiSystem> listSystemsOrdered() {
        return systemMapper.selectList(new LambdaQueryWrapper<IbmiSystem>()
                .orderByAsc(IbmiSystem::getSortOrder));
    }

    /** 未关闭告警数（status=OPEN） */
    public long countOpenAlerts() {
        return alertEventMapper.selectCount(
                new LambdaQueryWrapper<AlertEvent>().eq(AlertEvent::getStatus, "OPEN"));
    }

    /**
     * 【R2】【P3】并行探测全部服务器并返回逐台结果：
     * 缓存命中的直接取快照，未命中的提交 healthProbePool 并行探测；
     * 外层 allOf 不设硬超时（单台超时由 AS400 客户端自身控制），
     * 单台异常在 probeServer 内 try/catch 降级为 FAIL+原因，不影响其余台数。
     */
    public List<HealthReportVO.ServerHealthVO> probeServers() {
        List<IbmiSystem> systems = listSystemsOrdered();
        if (systems.isEmpty()) {
            return List.of();
        }
        // 先查 30s 缓存，仅未命中的才进线程池（避免无谓的池调度）
        Map<Long, HealthReportVO.ServerHealthVO> hits = new HashMap<>();
        Map<Long, CompletableFuture<HealthReportVO.ServerHealthVO>> probes = new LinkedHashMap<>();
        for (IbmiSystem system : systems) {
            HealthReportVO.ServerHealthVO cached = serverProbeCache.getIfPresent(system.getId());
            if (cached != null) {
                hits.put(system.getId(), cached);
            } else {
                probes.put(system.getId(),
                        CompletableFuture.supplyAsync(() -> probeServer(system), healthProbePool));
            }
        }
        CompletableFuture.allOf(probes.values().toArray(new CompletableFuture[0])).join();
        // 按 listSystemsOrdered 原顺序回填结果；仅新探测结果写缓存（put 会重置 TTL，命中项不能回写）
        List<HealthReportVO.ServerHealthVO> result = new ArrayList<>(systems.size());
        for (IbmiSystem system : systems) {
            HealthReportVO.ServerHealthVO hit = hits.get(system.getId());
            if (hit != null) {
                result.add(hit);
            } else {
                HealthReportVO.ServerHealthVO fresh = probes.get(system.getId()).join();
                serverProbeCache.put(system.getId(), fresh);
                result.add(fresh);
            }
        }
        return result;
    }

    /** 单台连接测试（任何异常降级为 FAIL 行，保持 VO 字段语义） */
    private HealthReportVO.ServerHealthVO probeServer(IbmiSystem system) {
        String connect;
        String detail;
        try {
            CommandResult result = clientProvider.forServer(system.getId()).testConnection();
            connect = result.success() ? "OK" : "FAIL";
            detail = result.message();
        } catch (Exception e) {
            connect = "FAIL";
            // 【E15】detail 不透传原始异常信息（可能含主机/端口等内部细节），只暴露异常类名
            detail = "Probe failed: " + e.getClass().getSimpleName();
            // 【E15】完整异常进 warn 日志，便于排查
            log.warn("[健康] 服务器 {} 探测失败", system.getName(), e);
        }
        return new HealthReportVO.ServerHealthVO(
                system.getId(), system.getName(), system.getHost(),
                system.getEnvironment(), connect, detail);
    }

    /**
     * 【R2】一站式巡检报告组装（原 HealthController.report 编排整体迁入）。
     * 聚合 DB / Quartz 调度器 / 各服务器连接（并行+缓存） / 未关闭告警 / 磁盘 / JVM 内存。
     */
    public HealthReportVO reportWithServers() {
        // 数据库连通性（轻量探测）
        String database = probeDatabase() ? "OK" : "DOWN";

        // Quartz 调度器状态
        boolean schedulerOk;
        try {
            schedulerOk = scheduler.isStarted();
        } catch (SchedulerException e) {
            schedulerOk = false;
        }
        String schedulerStatus = schedulerOk ? "RUNNING" : "STOPPED";

        return new HealthReportVO(
                LocalDateTime.now(), database, schedulerStatus,
                probeServers(), countOpenAlerts(),
                getDiskSpaceInfo(), getMemoryInfo());
    }

    private HealthReportVO.DiskSpaceVO getDiskSpaceInfo() {
        try {
            File root = new File("/");
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            return new HealthReportVO.DiskSpaceVO("UP", total, free,
                    formatBytes(total), formatBytes(free));
        } catch (Exception e) {
            return new HealthReportVO.DiskSpaceVO("DOWN", 0, 0, "0 B", "0 B");
        }
    }

    private HealthReportVO.MemoryVO getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return new HealthReportVO.MemoryVO("UP", used, max,
                formatBytes(used), formatBytes(max));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
