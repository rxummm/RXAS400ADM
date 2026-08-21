package com.rxas400adm.monitor.service;

import java.util.Map;

public interface ICapacityService {

    Map<String, Object> trend(Long instanceId, int days);
}