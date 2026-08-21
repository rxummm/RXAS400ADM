package com.rxas400adm.monitor.service;

import java.util.List;
import java.util.Map;

public interface IBaselineService {

    void computeBaseline(Long instanceId);

    List<Map<String, Object>> baselineWithCurrent(Long instanceId);
}