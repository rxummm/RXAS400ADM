package com.rxas400adm.as400.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.entity.CommandScript;
import com.rxas400adm.as400.entity.JobSchedule;
import com.rxas400adm.as400.entity.JobScheduleHistory;
import com.rxas400adm.as400.mapper.CommandScriptMapper;
import com.rxas400adm.as400.mapper.JobScheduleHistoryMapper;
import com.rxas400adm.as400.mapper.JobScheduleMapper;
import com.rxas400adm.as400.vo.ExecutionStatsVO;
import com.rxas400adm.common.constants.ExecutionStatus;
import com.rxas400adm.common.constants.PageConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 执行历史聚合服务（R1 分层清零）：作业调度执行历史 + 命令脚本执行记录，
 * 按来源/状态/关键字过滤，供 ExecutionController 组装分页。
 */
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final JobScheduleHistoryMapper historyMapper;
    private final JobScheduleMapper scheduleMapper;
    private final CommandScriptMapper scriptMapper;

    /** 调度执行历史：关联任务名/类型/服务器/操作人（P2-11 批量预取，无 N+1） */
    public List<Map<String, Object>> scheduleExecutions(String status, String keyword, int limit) {
        LambdaQueryWrapper<JobScheduleHistory> wrapper = new LambdaQueryWrapper<JobScheduleHistory>()
                .orderByDesc(JobScheduleHistory::getRunTime)
                .last(PageConstants.limitClause(clampLimit(limit)));
        if (status != null && !status.isBlank()) {
            wrapper.eq(JobScheduleHistory::getStatus, status.trim().toUpperCase());
        }
        List<JobScheduleHistory> histories = historyMapper.selectList(wrapper);
        // B1：空结果守卫——selectBatchIds 不接受空集合（生成非法 IN ()），直接返回空页
        List<Long> scheduleIds = histories.stream().map(JobScheduleHistory::getScheduleId).distinct().toList();
        Map<Long, JobSchedule> scheduleById = scheduleIds.isEmpty() ? Map.of()
                : scheduleMapper.selectBatchIds(scheduleIds)
                        .stream().collect(Collectors.toMap(JobSchedule::getId, s -> s, (a, b) -> b));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (JobScheduleHistory h : histories) {
            JobSchedule schedule = scheduleById.get(h.getScheduleId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("source", "SCHEDULE");
            row.put("id", h.getId());
            row.put("taskId", h.getScheduleId());
            String name = schedule == null ? "#" + h.getScheduleId() : schedule.getName();
            String user = schedule == null ? null : schedule.getCreatedBy();
            row.put("name", name);
            row.put("type", schedule == null ? "?" : schedule.getScheduleType());
            row.put("serverId", schedule == null ? null : schedule.getServerId());
            row.put("user", user);
            row.put("runTime", h.getRunTime());
            row.put("status", h.getStatus());
            row.put("message", h.getMessage());
            row.put("costMs", h.getCostMs());
            if (matchKeyword(row, keyword)) {
                rows.add(row);
            }
        }
        return rows;
    }

    /** 命令脚本执行记录：仅最近一次执行（脚本未做逐次历史） */
    public List<Map<String, Object>> scriptExecutions(String status, String keyword, int limit) {
        LambdaQueryWrapper<CommandScript> wrapper = new LambdaQueryWrapper<CommandScript>()
                .isNotNull(CommandScript::getLastRunTime)
                .orderByDesc(CommandScript::getLastRunTime)
                .last(PageConstants.limitClause(clampLimit(limit)));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CommandScript s : scriptMapper.selectList(wrapper)) {
            // M1：读结构化状态（V44 回填，老数据也有值）；空值防御性按 SUCCESS 处理
            String resultStatus = ExecutionStatus.SUCCESS.equalsIgnoreCase(s.getLastRunStatus())
                    ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
            if (status != null && !status.isBlank() && !resultStatus.equalsIgnoreCase(status.trim())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("source", "SCRIPT");
            row.put("id", s.getId());
            row.put("taskId", s.getId());
            row.put("name", s.getName());
            row.put("type", "CL");
            row.put("serverId", null);
            row.put("user", s.getCreatedBy());
            row.put("runTime", s.getLastRunTime());
            row.put("status", resultStatus);
            row.put("message", s.getLastResult());
            row.put("costMs", null);
            row.put("runCount", s.getRunCount());
            if (matchKeyword(row, keyword)) {
                rows.add(row);
            }
        }
        return rows;
    }

    /** 获取执行统计数据 */
    public ExecutionStatsVO getStats() {
        // P16c 统计下推：一条聚合 SQL 直出两表统计，替代原「各拉 500 条进堆再内存汇总」
        Map<String, Object> row = historyMapper.selectExecutionStats();
        if (row == null) { // 聚合无 GROUP BY 恒返单行，防御性兜底空 Map
            row = Map.of();
        }
        long total = ((Number) row.getOrDefault("totalCnt", 0)).longValue();
        long success = ((Number) row.getOrDefault("successCnt", 0)).longValue();
        long failed = total - success;
        double successRate = total > 0 ? (double) success / total * 100 : 0;

        // 空表 AVG 为 NULL → 按 0 处理
        Object avgRaw = row.get("avgCost");
        double avgCostMs = avgRaw instanceof Number n ? n.doubleValue() : 0;

        return new ExecutionStatsVO(total, success, failed, successRate, avgCostMs);
    }

    private boolean matchKeyword(Map<String, Object> row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String kw = keyword.trim().toLowerCase();
        Object name = row.get("name");
        Object user = row.get("user");
        Object message = row.get("message");
        return (name != null && String.valueOf(name).toLowerCase().contains(kw))
                || (user != null && String.valueOf(user).toLowerCase().contains(kw))
                || (message != null && String.valueOf(message).toLowerCase().contains(kw));
    }

    /** L7：服务层收敛 limit 到 [1, MAX_LIMIT]（原仅 Controller 钳制，防止未来调用方直传非法值） */
    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, PageConstants.MAX_LIMIT));
    }
}
