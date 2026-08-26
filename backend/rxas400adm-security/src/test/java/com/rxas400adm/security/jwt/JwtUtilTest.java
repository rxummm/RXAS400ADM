package com.rxas400adm.security.jwt;

import com.rxas400adm.security.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    /** R7：JwtUtil 构造改为注入 JwtProperties——测试同步适配，字段取值与原构造实参逐字一致 */
    private static JwtProperties jwtProperties(long expireMs) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("RXAS400-Enterprise-IBM-i-Operation-Platform-Secret-2026");
        properties.setExpireMs(expireMs);
        properties.setRefreshExpireMs(604800000L);
        properties.setIssuer("rxas400adm");
        properties.setAudience("rxas400-ui");
        return properties;
    }

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(jwtProperties(3600000L));
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
        JwtUtil shortLived = new JwtUtil(jwtProperties(0L));
        String token = shortLived.generateToken("admin", List.of());
        assertFalse(shortLived.isValid(token));
    }
}