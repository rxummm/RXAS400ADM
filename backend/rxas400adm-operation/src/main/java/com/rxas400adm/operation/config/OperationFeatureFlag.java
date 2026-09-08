package com.rxas400adm.operation.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperationFeatureFlag {

    private final OperationProperties properties;

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isTypeEnabled(String operationType) {
        if (!properties.isEnabled()) {
            return false;
        }
        return properties.getTypes().getOrDefault(operationType, false);
    }

    public boolean shouldUseOperationMode(String operationType) {
        return isTypeEnabled(operationType);
    }
}