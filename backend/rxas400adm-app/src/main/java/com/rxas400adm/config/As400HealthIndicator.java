package com.rxas400adm.config;

import com.rxas400adm.config.vo.HealthReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AS400 服务器连接健康指示器：
 * - UP：至少一台服务器可达
 * - DOWN：所有服务器均不可达
 * <p>
 * 【P3】不再逐台直连，复用 HealthService.probeServers() 的并行探测 + 30s Caffeine 缓存
 * （与 REST 巡检 /api/v1/health 共用同一份快照），单台失败已在服务层降级为 FAIL 行。
 */
@Component("as400")
@RequiredArgsConstructor
public class As400HealthIndicator implements HealthIndicator {

    private final HealthService healthService;

    @Override
    public Health health() {
        List<HealthReportVO.ServerHealthVO> servers = healthService.probeServers();
        int total = servers.size();
        int ok = 0;
        for (HealthReportVO.ServerHealthVO server : servers) {
            if ("OK".equals(server.connect())) {
                ok++;
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
