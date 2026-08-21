package com.rxas400adm.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** L2：SSRF 守卫单测——公网放行，私网/回环/链路本地/任意/组播/CGNAT/非 http(s) 拒绝。 */
class SsrfGuardTest {

    @Test
    void publicIpLiteral_shouldBeAllowed() {
        assertTrue(SsrfGuard.isSafeUrl("http://8.8.8.8/x"));
        assertTrue(SsrfGuard.isSafeUrl("https://1.1.1.1/hook"));
    }

    @Test
    void loopback_shouldBeBlocked() {
        assertFalse(SsrfGuard.isSafeUrl("http://127.0.0.1/x"));
        assertFalse(SsrfGuard.isSafeUrl("http://localhost/x"));
        assertFalse(SsrfGuard.isSafeUrl("http://[::1]/x"));
    }

    @Test
    void cloudMetadata_shouldBeBlocked() {
        assertFalse(SsrfGuard.isSafeUrl("http://169.254.169.254/latest/meta-data/"));
    }

    @Test
    void privateRanges_shouldBeBlocked() {
        assertFalse(SsrfGuard.isSafeUrl("http://10.0.0.1/x"));
        assertFalse(SsrfGuard.isSafeUrl("http://172.16.0.1/x"));
        assertFalse(SsrfGuard.isSafeUrl("http://192.168.1.10/x"));
        assertFalse(SsrfGuard.isSafeUrl("http://100.64.0.1/x")); // CGNAT
        assertFalse(SsrfGuard.isSafeUrl("http://[fc00::1]/x"));  // IPv6 ULA
    }

    @Test
    void anyAndMulticast_shouldBeBlocked() {
        assertFalse(SsrfGuard.isSafeUrl("http://0.0.0.0/x"));
        assertFalse(SsrfGuard.isSafeUrl("http://224.0.0.1/x"));
    }

    @Test
    void nonHttpSchemeOrMalformed_shouldBeBlocked() {
        assertFalse(SsrfGuard.isSafeUrl("ftp://8.8.8.8/x"));
        assertFalse(SsrfGuard.isSafeUrl("file:///etc/passwd"));
        assertFalse(SsrfGuard.isSafeUrl(""));
        assertFalse(SsrfGuard.isSafeUrl("not-a-url"));
        assertFalse(SsrfGuard.isSafeUrl("http:///no-host"));
    }
}