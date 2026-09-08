package com.rxas400adm.operation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "rxas400.operation")
public class OperationProperties {
    private boolean enabled = false;
    private Map<String, Boolean> types = new HashMap<>();
    private int maxRetry = 3;
    private long cleanupIntervalMs = 60_000;
    private int stuckThresholdMinutes = 10;
    private Map<String, Long> timeouts = new HashMap<>();

    public int getStuckThresholdMinutes() {
        return stuckThresholdMinutes;
    }

    public void setStuckThresholdMinutes(int stuckThresholdMinutes) {
        this.stuckThresholdMinutes = stuckThresholdMinutes;
    }
}