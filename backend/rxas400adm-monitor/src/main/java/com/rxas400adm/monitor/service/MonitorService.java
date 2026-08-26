package com.rxas400adm.monitor.service;

import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.service.IIbmiSystemService;
import com.rxas400adm.monitor.vo.CompareResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 监控中心编排服务。
 * <p>
 * 【R2】MonitorController.compare 的组装循环下沉至此，Controller 只保留参数接收 +
 * 委托 + ApiResponse 包装。
 * 【P4】逐台 overview 由串行改为 healthProbePool 并行 fan-out（与健康巡检共用并行度），
 * 总耗时≈最慢一台；单台异常降级为占位行（字段填空值），不拖垮整页对比。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorService {

    /** 服务器对比最大数量（P2-17，随编排逻辑自 Controller 下沉） */
    private static final int MAX_COMPARE_IDS = 20;

    private final IIbmiSystemService ibmiSystemService;
    private final IMetricService metricService;
    /** 与 HealthService 巡检共用的探测池（bean 名与字段名一致，按名注入） */
    private final Executor healthProbePool;

    /**
     * 多服务器当前指标快照并排（借鉴旧项目 serverCompare，指标维度）：
     * P2-17 最多对比 20 台、超出静默截断；【P4】各台并行取数后按入参顺序回填。
     */
    public List<CompareResultVO> compare(List<Long> ids) {
        List<Long> targetIds = ids == null ? List.of()
                : ids.stream().distinct().limit(MAX_COMPARE_IDS).toList();
        if (targetIds.isEmpty()) {
            return List.of();
        }
        // 【P4】并行 fan-out：CallerRuns 兜底不丢任务；单台超时由底层客户端自身控制
        Map<Long, CompletableFuture<CompareResultVO>> futures = new LinkedHashMap<>();
        for (Long id : targetIds) {
            futures.put(id, CompletableFuture.supplyAsync(() -> compareOne(id), healthProbePool));
        }
        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();
        List<CompareResultVO> result = new ArrayList<>(targetIds.size());
        for (Long id : targetIds) {
            result.add(futures.get(id).join());
        }
        return result;
    }

    /** 单台组装：元信息 + overview；任何异常降级为占位行（沿用现字段填空值语义） */
    private CompareResultVO compareOne(Long id) {
        try {
            IbmiSystem system = ibmiSystemService.get(id);
            return new CompareResultVO(
                    id,
                    system == null ? "SERVER-" + id : system.getName(),
                    system == null ? "-" : system.getHost(),
                    system == null ? "-" : system.getEnvironment(),
                    system == null ? "-" : system.getStatus(),
                    metricService.overview(id));
        } catch (Exception e) {
            log.warn("[对比] 服务器 {} 指标获取失败，降级占位行", id, e);
            return new CompareResultVO(id, "SERVER-" + id, "-", "-", "-", Map.of());
        }
    }
}
