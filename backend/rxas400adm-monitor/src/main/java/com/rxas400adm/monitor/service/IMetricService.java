package com.rxas400adm.monitor.service;

import com.rxas400adm.monitor.domain.Metric;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface IMetricService {

    void save(Metric metric);

    /** P5：批量入库（空列表直接 return），供采集调度器每轮合并写库 */
    void saveBatch(List<Metric> metrics);

    Map<String, Object> overview(Long instanceId);

    List<Metric> history(Long instanceId, int limit);

    void cleanBefore(LocalDateTime time);
}