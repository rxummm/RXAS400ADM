package com.rxas400adm.operation.service;

import com.rxas400adm.operation.annotation.IbmiOperation;
import com.rxas400adm.operation.executor.OperationExecutor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationRegistry {

    private final ApplicationContext applicationContext;
    private final Map<String, OperationExecutor> registry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(IbmiOperation.class);
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            if (bean instanceof OperationExecutor executor) {
                IbmiOperation annotation = executor.getClass().getAnnotation(IbmiOperation.class);
                if (annotation != null) {
                    registry.put(annotation.code(), executor);
                    log.info("Registered Operation: code={}, riskLevel={}, class={}",
                        annotation.code(), annotation.riskLevel(), executor.getClass().getSimpleName());
                }
            }
        }
        log.info("OperationRegistry initialized with {} operations", registry.size());
    }

    public OperationExecutor get(String code) {
        return registry.get(code);
    }

    public Map<String, OperationExecutor> getAll() {
        return Map.copyOf(registry);
    }
}