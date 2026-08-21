package com.rxas400adm.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.monitor.domain.Metric;
import com.rxas400adm.monitor.mapper.MetricMapper;
import com.rxas400adm.monitor.websocket.MetricPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetricService implements IMetricService {

    private static final String ACTIVE_JOB_SQL =
            "SELECT JOB_NAME FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) X";

    private final MetricMapper metricMapper;
    private final MetricPublisher publisher;
    private final AS400ClientProvider clientProvider;

    public void save(Metric metric) {
        metricMapper.insert(metric);
        publisher.publish(metric);
    }

    /** 总览：{ cpu, memory, disk, jobs, msgw, lckw }，均取实时/最新值 */
    public Map<String, Object> overview(Long instanceId) {
        Map<String, Object> result = new LinkedHashMap<>();
        // 一次查询取出 5 项指标最新值（替代 5 次独立 SELECT）
        List<Map<String, Object>> rows = metricMapper.selectLatestOverview(instanceId);
        Map<String, Double> latestMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String name = (String) row.get("metricName");
            Object val = row.get("metricValue");
            latestMap.put(name, val instanceof Number ? ((Number) val).doubleValue() : 0.0);
        }
        result.put("cpu", latestMap.getOrDefault("CPU", 0.0));
        result.put("memory", latestMap.getOrDefault("MEMORY", 0.0));
        result.put("disk", latestMap.getOrDefault("DISK", 0.0));
        result.put("msgw", latestMap.getOrDefault("MSGW", 0.0));
        result.put("lckw", latestMap.getOrDefault("LCKW", 0.0));
        try {
            AS400Client client = clientProvider.forServer(instanceId);
            result.put("jobs", client.queryList(ACTIVE_JOB_SQL).size());
        } catch (Exception e) {
            result.put("jobs", 0);
        }
        return result;
    }

    /** 最近 N 条历史指标 */
    public List<Metric> history(Long instanceId, int limit) {
        return metricMapper.selectList(new LambdaQueryWrapper<Metric>()
                .eq(Metric::getInstanceId, instanceId)
                .orderByDesc(Metric::getCollectTime)
                .last(PageConstants.limitClause(Math.max(1, Math.min(limit, 500)))));
    }

    public void cleanBefore(LocalDateTime time) {
        metricMapper.delete(new LambdaQueryWrapper<Metric>().lt(Metric::getCollectTime, time));
    }
}