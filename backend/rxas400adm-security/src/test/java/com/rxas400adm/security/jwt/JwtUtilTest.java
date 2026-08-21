package com.rxas400adm.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "RXAS400-Enterprise-IBM-i-Operation-Platform-Secret-2026", 3600000L, 604800000L, "rxas400adm", "rxas400-ui");
    }

    @Test
    void generateAndParse_shouldRoundTrip() {
        String token = jwtUtil.generateToken("admin", List.of("JOB_VIEW", "USER_MANAGE"));
        assertTrue(jwtUtil.isValid(token));
        assertEquals("admin", jwtUtil.getUsername(token));
        assertEquals(List.of("JOB_VIEW", "USER_MANAGE"), jwtUtil.getPermissions(token));
    }

    @Test
    void tokenWithoutPermissions_shouldReturnEmptyList() {
        String token = jwtUtil.generateToken("viewer", List.of());
        assertTrue(jwtUtil.getPermissions(token).isEmpty());
    }

    @Test
    void tamperedToken_shouldBeInvalid() {
        String token = jwtUtil.generateToken("admin", List.of());
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertFalse(jwtUtil.isValid(tampered));
    }

    @Test
    void expiredToken_shouldBeInvalid() {
        JwtUtil shortLived = new JwtUtil(
                "RXAS400-Enterprise-IBM-i-Operation-Platform-Secret-2026", 0L, 604800000L, "rxas400adm", "rxas400-ui");
        String token = shortLived.generateToken("admin", List.of());
        assertFalse(shortLived.isValid(token));
    }
}