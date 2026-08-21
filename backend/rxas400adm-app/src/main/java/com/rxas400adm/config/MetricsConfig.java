package com.rxas400adm.config;

import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.service.IIbmiSystemService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义业务指标注册：
 * - as400.servers.total：配置的 AS400 服务器总数
 * - as400.servers.enabled：启用的 AS400 服务器数
 *
 * 暴露在 /actuator/prometheus，可被 Prometheus 采集并在 Grafana 看板展示。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final IIbmiSystemService ibmiSystemService;

    @Bean
    public MeterBinder as400ServerMetrics() {
        return registry -> {
            Gauge.builder("as400.servers.total", ibmiSystemService, svc -> {
                        try {
                            return (double) svc.list().size();
                        } catch (Exception e) {
                            return 0.0;
                        }
                    })
                    .description("Total configured AS400 servers")
                    .tag("application", "rxas400adm")
                    .register(registry);

            Gauge.builder("as400.servers.enabled", ibmiSystemService, svc -> {
                        try {
                            return svc.list().stream()
                                    .filter(IbmiSystem::getEnabled)
                                    .count();
                        } catch (Exception e) {
                            return 0.0;
                        }
                    })
                    .description("Enabled AS400 servers")
                    .tag("application", "rxas400adm")
                    .register(registry);
        };
    }
}
