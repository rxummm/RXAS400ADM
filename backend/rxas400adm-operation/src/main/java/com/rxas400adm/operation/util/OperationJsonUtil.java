package com.rxas400adm.operation.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxas400adm.operation.domain.Operation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class OperationJsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OperationJsonUtil() {
    }

    public static String extractField(Operation op, String fieldName) {
        try {
            JsonNode node = MAPPER.readTree(op.getRequestData());
            return node.has(fieldName) ? node.get(fieldName).asText() : null;
        } catch (Exception e) {
            log.warn("Failed to extract field '{}' from operation requestData (opId={}): {}",
                fieldName, op.getId(), e.getMessage());
            return null;
        }
    }
}