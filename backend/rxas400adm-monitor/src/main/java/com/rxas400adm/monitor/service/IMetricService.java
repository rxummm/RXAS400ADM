package com.rxas400adm.monitor.service;

import com.rxas400adm.monitor.domain.Metric;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface IMetricService {

    void save(Metric metric);

    Map<String, Object> overview(Long instanceId);

    List<Metric> history(Long instanceId, int limit);

    void cleanBefore(LocalDateTime time);
}