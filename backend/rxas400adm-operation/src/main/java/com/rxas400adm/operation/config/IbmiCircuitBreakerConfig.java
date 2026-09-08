package com.rxas400adm.operation.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class IbmiCircuitBreakerConfig {

    private static final float FAILURE_RATE_THRESHOLD = 50.0f;
    private static final int WAIT_DURATION_SECONDS = 30;
    private static final int SLIDING_WINDOW_SIZE = 10;
    private static final int MINIMUM_NUMBER_OF_CALLS = 5;
    private static final int PERMITTED_CALLS_IN_HALF_OPEN = 3;

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(FAILURE_RATE_THRESHOLD)
            .waitDurationInOpenState(Duration.ofSeconds(WAIT_DURATION_SECONDS))
            .slidingWindowSize(SLIDING_WINDOW_SIZE)
            .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
            .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS_IN_HALF_OPEN)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();

        return CircuitBreakerRegistry.of(config);
    }
}