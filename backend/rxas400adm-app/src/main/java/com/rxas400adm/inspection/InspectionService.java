package com.rxas400adm.inspection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.model.SubsystemRow;
import com.rxas400adm.as400.service.IIbmiSystemService;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.monitor.alert.AlertEvent;
import com.rxas400adm.monitor.mapper.AlertEventMapper;
import com.rxas400adm.monitor.service.IMetricService;
import com.rxas400adm.report.IReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 巡检报告（借鉴旧项目 inspectionReport）：对单台 AS400 做综合健康巡检，
 * 聚合 CPU/内存/磁盘/ASP/子系统/作业/消息等待/最近告警，产出总分与问题清单，
 * 可导出 Excel/PDF（复用报表引擎 ReportService）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionService implements IInspectionService {

    private final AS400ClientProvider clientProvider;
    private final IIbmiSystemService systemService;
    private final IMetricService metricService;
    private final AlertEventMapper alertEventMapper;
    private final IReportService reportService;

    /** 生成巡检报告 JSON（不落库，按需生成） */
    public Map<String, Object> generate(Long serverId) {
        AS400Client client = clientProvider.forServer(serverId);
        IbmiSystem system = systemService.get(serverId);
        Map<String, Object> overview = metricService.overview(serverId);
        List<Map<String, Object>> asp = safeList(client.queryList("SELECT * FROM QSYS2.ASP_INFO"));
        List<SubsystemRow> subsystems = client.listSubsystems() == null ? List.of() : client.listSubsystems();
        List<AlertEvent> alerts = alertEventMapper.selectList(new LambdaQueryWrapper<AlertEvent>()
                .eq(AlertEvent::getInstanceId, serverId)
                .orderByDesc(AlertEvent::getCreatedTime)
                .last(PageConstants.limitClause(10)));

        List<Map<String, Object>> checks = new ArrayList<>();
        List<Map<String, Object>> issues = new ArrayList<>();
        double score = 100;

        // 1) CPU
        double cpu = num(overview.get("cpu"));
        checks.add(check("CPU_USAGE", cpu + "%", cpu >= 90 ? "CRITICAL" : cpu >= 80 ? "WARNING" : "OK"));
        if (cpu >= 90) { score -= 30; issue(issues, "CPU", "CRITICAL", "CPU_OVER_90", Map.of("value", String.format("%.1f", cpu))); }
        else if (cpu >= 80) { score -= 15; issue(issues, "CPU", "WARNING", "CPU_OVER_80", Map.of("value", String.format("%.1f", cpu))); }

        // 2) 内存
        double mem = num(overview.get("memory"));
        checks.add(check("MEMORY_USAGE", mem + "%", mem >= 90 ? "CRITICAL" : mem >= 85 ? "WARNING" : "OK"));
        if (mem >= 90) { score -= 20; issue(issues, "MEMORY", "CRITICAL", "MEMORY_OVER_90", Map.of("value", String.format("%.1f", mem))); }
        else if (mem >= 85) { score -= 10; issue(issues, "MEMORY", "WARNING", "MEMORY_OVER_85", Map.of("value", String.format("%.1f", mem))); }

        // 3) 磁盘（总览 DISK）+ ASP 明细
        double disk = num(overview.get("disk"));
        checks.add(check("DISK_USAGE", disk + "%", disk >= 90 ? "CRITICAL" : disk >= 80 ? "WARNING" : "OK"));
        if (disk >= 90) { score -= 20; issue(issues, "DISK", "CRITICAL", "DISK_OVER_90", Map.of("value", String.format("%.1f", disk))); }
        else if (disk >= 80) { score -= 10; issue(issues, "DISK", "WARNING", "DISK_OVER_80", Map.of("value", String.format("%.1f", disk))); }
        for (Map<String, Object> a : asp) {
            double used = num(a.get("USED_SPACE"));
            double total = num(a.get("TOTAL_SPACE"));
            if (total <= 0) continue;
            double pct = used / total * 100;
            String aspName = String.valueOf(a.getOrDefault("ASP_NAME", "?"));
            if (pct >= 90) { score -= 10; issue(issues, "ASP", "CRITICAL", "ASP_OVER_90", Map.of("aspName", aspName, "value", String.format("%.1f", pct))); }
            else if (pct >= 80) { score -= 5; issue(issues, "ASP", "WARNING", "ASP_OVER_80", Map.of("aspName", aspName, "value", String.format("%.1f", pct))); }
        }

        // 4) 子系统
        long activeSub = subsystems.stream().filter(s -> "ACTIVE".equalsIgnoreCase(String.valueOf(s.status()))).count();
        checks.add(check("SUBSYSTEM_STATUS", activeSub + "/" + subsystems.size() + " active",
                subsystems.isEmpty() ? "WARNING" : "OK"));
        if (subsystems.isEmpty()) { score -= 10; issue(issues, "SUBSYSTEM", "WARNING", "SUBSYSTEM_UNAVAILABLE", Map.of()); }
        else if (activeSub < subsystems.size()) { score -= 5; issue(issues, "SUBSYSTEM", "WARNING", "SUBSYSTEM_INACTIVE", Map.of("activeSub", String.valueOf(activeSub), "totalSub", String.valueOf(subsystems.size()))); }

        // 5) 作业
        double jobs = num(overview.get("jobs"));
        checks.add(check("ACTIVE_JOBS", (long) jobs + "", "OK"));

        // 6) 消息等待 / 锁等待
        double msgw = num(overview.get("msgw"));
        double lckw = num(overview.get("lckw"));
        checks.add(check("MSGW_COUNT", (long) msgw + "", msgw > 0 ? "WARNING" : "OK"));
        checks.add(check("LCKW_COUNT", (long) lckw + "", lckw > 0 ? "WARNING" : "OK"));
        if (msgw > 0) { score -= 5; issue(issues, "JOB", "WARNING", "JOB_MSGW", Map.of("count", String.valueOf((long) msgw))); }

        // 7) 最近告警
        long criticalAlerts = alerts.stream().filter(a -> "CRITICAL".equalsIgnoreCase(a.getLevel())).count();
        checks.add(check("RECENT_ALERTS", alerts.size() + " entries（CRITICAL " + criticalAlerts + "）",
                criticalAlerts > 0 ? "WARNING" : "OK"));
        score -= Math.min(30, criticalAlerts * 10);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("serverId", serverId);
        report.put("serverName", system == null ? "SERVER-" + serverId : system.getName());
        report.put("host", system == null ? "-" : system.getHost());
        report.put("environment", system == null ? "-" : system.getEnvironment());
        report.put("status", system == null ? "-" : system.getStatus());
        report.put("generatedAt", LocalDateTime.now().toString());
        report.put("score", Math.max(0, Math.min(100, (int) Math.round(score))));
        report.put("grade", grade((int) Math.max(0, Math.min(100, Math.round(score)))));
        report.put("checks", checks);
        report.put("issues", issues);
        return report;
    }

    /** 导出巡检报告（xlsx / pdf，复用报表引擎） */
    public byte[] export(String format, Long serverId) {
        Map<String, Object> report = generate(serverId);
        String title = "巡检报告-" + report.get("serverName") + "-" + LocalDateTime.now().toLocalDate();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object c : safeList(report.get("checks"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> check = (Map<String, Object>) c;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("check", check.get("name"));
            row.put("value", check.get("value"));
            row.put("status", check.get("status"));
            rows.add(row);
        }
        for (Object i : safeList(report.get("issues"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> issue = (Map<String, Object>) i;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("check", "⚠ " + issue.get("item"));
            row.put("value", issue.get("detail"));
            row.put("status", issue.get("level"));
            rows.add(row);
        }
        return reportService.render(format, title,
                new String[]{"check", "value", "status"}, rows);
    }

    private Map<String, Object> check(String name, String value, String status) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("name", name);
        c.put("value", value);
        c.put("status", status);
        return c;
    }

    private void issue(List<Map<String, Object>> issues, String item, String level, String detailCode, Map<String, Object> detailParams) {
        Map<String, Object> i = new LinkedHashMap<>();
        i.put("item", item);
        i.put("level", level);
        i.put("detail", detailCode);
        i.put("detailParams", detailParams);
        issues.add(i);
    }

    private String grade(int score) {
        return score >= 90 ? "A" : score >= 75 ? "B" : score >= 60 ? "C" : "D";
    }

    private double num(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            log.trace("parse num failed for '{}': {}", o, e.getMessage());
            return 0;
        }
    }

    private List<Map<String, Object>> safeList(Object o) {
        if (o instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) m;
                    result.add(map);
                }
            }
            return result;
        }
        return List.of();
    }
}