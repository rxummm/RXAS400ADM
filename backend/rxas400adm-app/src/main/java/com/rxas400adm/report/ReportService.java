package com.rxas400adm.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.entity.CommandScript;
import com.rxas400adm.as400.entity.JobSchedule;
import com.rxas400adm.as400.entity.JobScheduleHistory;
import com.rxas400adm.as400.mapper.CommandScriptMapper;
import com.rxas400adm.as400.mapper.JobScheduleHistoryMapper;
import com.rxas400adm.as400.mapper.JobScheduleMapper;
import com.rxas400adm.common.constants.ExecutionStatus;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.monitor.mapper.MetricMapper;
import com.rxas400adm.monitor.service.ICapacityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表引擎（3.11）：指标日报/周报、执行记录报表、容量趋势报表。
 * 本类只做数据准备与编排；Excel/PDF 渲染拆分至 {@link ReportRenderer}（中-3），
 * 标题/表头唯一事实源收敛在 {@link ReportSpec}（中-17）。
 */
@Service
@RequiredArgsConstructor
public class ReportService implements IReportService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MetricMapper metricMapper;
    private final JobScheduleHistoryMapper historyMapper;
    private final JobScheduleMapper scheduleMapper;
    private final CommandScriptMapper scriptMapper;
    private final ICapacityService capacityService;

    /* ---------------- 数据准备 ---------------- */

    /** 指标聚合报表：近 N 天按 指标+日期 聚合均值/峰值/最小值（P3：GROUP BY 下推到数据库） */
    public List<Map<String, Object>> metricsRows(Long instanceId, int days) {
        int window = Math.max(1, Math.min(days, 365));
        // 数据库侧按 DATE(collect_time)+metric_name 分组聚合，只回传少量汇总行
        // （旧实现全量加载原始采样后在 Java 侧聚合，采样量随监控粒度线性增长）
        List<Map<String, Object>> aggregated = metricMapper.selectAggregatedMetrics(instanceId, LocalDateTime.now().minusDays(window));
        List<Map<String, Object>> rows = new ArrayList<>(aggregated.size());
        for (Map<String, Object> a : aggregated) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", a.get("d"));
            row.put("metric", a.get("m"));
            row.put("avg", a.get("avg"));
            row.put("max", a.get("max"));
            row.put("min", a.get("min"));
            row.put("samples", a.get("samples"));
            rows.add(row);
        }
        return rows;
    }

    /** 执行记录报表：调度历史 + 脚本最近执行（与执行审计页同构） */
    public List<Map<String, Object>> executionRows(String type, String status) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (type == null || type.isBlank() || "SCHEDULE".equalsIgnoreCase(type)) {
            LambdaQueryWrapper<JobScheduleHistory> wrapper = new LambdaQueryWrapper<JobScheduleHistory>()
                    .orderByDesc(JobScheduleHistory::getRunTime)
                    .last(PageConstants.limitClause(500));
            if (status != null && !status.isBlank()) {
                wrapper.eq(JobScheduleHistory::getStatus, status.trim().toUpperCase());
            }
            // N5：先收集 history 与 scheduleId，再批量查调度定义，避免逐行 selectById 的 N+1
            List<JobScheduleHistory> histories = historyMapper.selectList(wrapper);
            List<Long> scheduleIds = histories.stream().map(JobScheduleHistory::getScheduleId).toList();
            Map<Long, JobSchedule> scheduleMap = scheduleIds.isEmpty() ? Map.of()
                    : scheduleMapper.selectBatchIds(scheduleIds).stream()
                            .collect(Collectors.toMap(JobSchedule::getId, s -> s));
            for (JobScheduleHistory h : histories) {
                JobSchedule schedule = scheduleMap.get(h.getScheduleId());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("time", h.getRunTime() == null ? "" : h.getRunTime().format(DATETIME_FMT));
                row.put("source", "SCHEDULE");
                row.put("name", schedule == null ? "#" + h.getScheduleId() : schedule.getName());
                row.put("type", schedule == null ? "?" : schedule.getScheduleType());
                row.put("user", schedule == null ? null : schedule.getCreatedBy());
                row.put("server", schedule == null ? null : schedule.getServerId());
                row.put("status", h.getStatus());
                row.put("message", h.getMessage());
                row.put("costMs", h.getCostMs());
                rows.add(row);
            }
        }
        if (type == null || type.isBlank() || "SCRIPT".equalsIgnoreCase(type)) {
            LambdaQueryWrapper<CommandScript> wrapper = new LambdaQueryWrapper<CommandScript>()
                    .isNotNull(CommandScript::getLastRunTime)
                    .orderByDesc(CommandScript::getLastRunTime)
                    .last(PageConstants.limitClause(200));
            for (CommandScript s : scriptMapper.selectList(wrapper)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("time", s.getLastRunTime() == null ? "" : s.getLastRunTime().format(DATETIME_FMT));
                row.put("source", "SCRIPT");
                row.put("name", s.getName());
                row.put("type", "CL");
                row.put("user", s.getCreatedBy());
                row.put("server", null);
                row.put("status", ExecutionStatus.SUCCESS.equalsIgnoreCase(s.getLastRunStatus())
                        ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED);
                row.put("message", s.getLastResult());
                row.put("costMs", null);
                rows.add(row);
            }
        }
        return rows;
    }

    /** 容量趋势报表：历史日均/峰值 + 未来 30 天预测 */
    public List<Map<String, Object>> capacityRows(Long instanceId, int days) {
        Map<String, Object> trend = capacityService.trend(instanceId, days);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object p : (List<?>) trend.get("points")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> point = (Map<String, Object>) p;
            Map<String, Object> row = new LinkedHashMap<>(point);
            row.put("kind", "历史");
            rows.add(row);
        }
        for (Object p : (List<?>) trend.get("prediction")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> point = (Map<String, Object>) p;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", point.get("date"));
            row.put("avg", point.get("value"));
            row.put("max", "-");
            row.put("kind", "预测");
            rows.add(row);
        }
        return rows;
    }

    /** 按报表类型统一生成（定时任务与手动导出共用）：返回渲染后的字节，失败返回空数组 */
    public byte[] generate(String reportType, String format, Long serverId, int days) {
        return switch (reportType == null ? "" : reportType.toLowerCase()) {
            case "metrics" -> {
                ReportSpec spec = ReportSpec.metrics(serverId, days);
                yield ReportRenderer.render(format, spec.title(), spec.headerArray(),
                        metricsRows(serverId == null ? 1 : serverId, days));
            }
            case "capacity" -> {
                ReportSpec spec = ReportSpec.capacity(serverId);
                yield ReportRenderer.render(format, spec.title(), spec.headerArray(),
                        capacityRows(serverId == null ? 1 : serverId, days));
            }
            default -> {
                ReportSpec spec = ReportSpec.executions();
                yield ReportRenderer.render(format, spec.title(), spec.headerArray(),
                        executionRows(null, null));
            }
        };
    }

    /* ---------------- 渲染（委托 ReportRenderer） ---------------- */

    public byte[] render(String format, String title, String[] headers, List<Map<String, Object>> rows) {
        return ReportRenderer.render(format, title, headers, rows);
    }
}
