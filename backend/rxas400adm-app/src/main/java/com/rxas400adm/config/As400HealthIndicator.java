package com.rxas400adm.config;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.entity.IbmiSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * AS400 服务器连接健康指示器：
 * - UP：至少一台服务器可达
 * - DOWN：所有服务器均不可达
 */
@Slf4j
@Component("as400")
@RequiredArgsConstructor
public class As400HealthIndicator implements HealthIndicator {

    private final AS400ClientProvider clientProvider;
    private final HealthService healthService;

    @Override
    public Health health() {
        int total = 0;
        int ok = 0;
        for (IbmiSystem system : healthService.listSystemsOrdered()) {
            total++;
            try {
                var result = clientProvider.forServer(system.getId()).testConnection();
                if (result.success()) {
                    ok++;
                }
            } catch (Exception e) {
                log.debug("AS400 health check failed for {}: {}", system.getName(), e.getMessage());
            }
        }
        if (total == 0) {
            return Health.unknown().withDetail("reason", "no AS400 servers configured").build();
        }
        if (ok == 0) {
            return Health.down().withDetail("reachable", 0).withDetail("total", total).build();
        }
        return Health.up().withDetail("reachable", ok).withDetail("total", total).build();
    }
}
