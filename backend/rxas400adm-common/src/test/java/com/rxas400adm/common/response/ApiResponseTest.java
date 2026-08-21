package com.rxas400adm.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void success_shouldReturnCodeZero() {
        ApiResponse<String> response = ApiResponse.success("hello");
        assertEquals(0, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("hello", response.getData());
    }

    @Test
    void error_shouldCarryCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.error(403, "无权限访问");
        assertEquals(403, response.getCode());
        assertEquals("无权限访问", response.getMessage());
    }

    @Test
    void serializedShape_shouldMatchUnifiedFormat() throws Exception {
        ApiResponse<Map<String, Object>> response = ApiResponse.success(Map.of("cpu", 42.5));
        String json = objectMapper.writeValueAsString(response);
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
        assertEquals(0, parsed.get("code"));
        assertEquals("success", parsed.get("message"));
        assertEquals(42.5, ((Map<?, ?>) parsed.get("data")).get("cpu"));
    }
}
