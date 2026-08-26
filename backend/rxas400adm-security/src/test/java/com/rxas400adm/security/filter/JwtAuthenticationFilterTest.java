package com.rxas400adm.security.filter;

import com.rxas400adm.security.config.JwtProperties;
import com.rxas400adm.security.config.ProxyProperties;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.PermissionService;
import com.rxas400adm.security.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * S1 安全回归：禁用/删除用户凭旧 token 不得继续访问。
 * - 数据库返回空权限（用户被删除/禁用）→ 不回退 token 内嵌权限，authorities 为空
 * - 数据库加载抛异常 → 回退 token 内嵌权限（可用性兜底）
 * - 数据库返回正常权限 → 使用数据库权限
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PermissionService permissionService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private JwtAuthenticationFilter filter;

    // R7：@Value 字段移除后，开关经 Properties 实例注入构造器（默认值与原 @Value 默认逐字一致）
    private final JwtProperties jwtProperties = new JwtProperties();
    private final ProxyProperties proxyProperties = new ProxyProperties();

    @BeforeEach
    void setUp() {
        // P2-1：黑名单检查默认开启（JwtProperties.blacklistEnabled 默认即 true）
        filter = new JwtAuthenticationFilter(jwtUtil, permissionService, tokenBlacklistService,
                jwtProperties, proxyProperties);
        SecurityContextHolder.clearContext();
    }

    private Authentication doFilter(String headerValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (headerValue != null) {
            request.addHeader("Authorization", headerValue);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void noHeader_shouldNotAuthenticate() throws Exception {
        assertNull(doFilter(null));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void invalidToken_shouldNotAuthenticate() throws Exception {
        when(jwtUtil.isValid("bad")).thenReturn(false);
        assertNull(doFilter("Bearer bad"));
    }

    @Test
    void blacklistedToken_shouldNotAuthenticate() throws Exception {
        // P2-1：登出吊销——jti 在黑名单中的 token 按匿名处理
        when(jwtUtil.isValid("revoked")).thenReturn(true);
        when(jwtUtil.getJti("revoked")).thenReturn("jti-1");
        when(tokenBlacklistService.isBlacklisted("jti-1")).thenReturn(true);
        assertNull(doFilter("Bearer revoked"));
        verifyNoInteractions(permissionService);
    }

    @Test
    void validToken_dbPermissions_shouldUseDbPermissions() throws Exception {
        when(jwtUtil.isValid("good")).thenReturn(true);
        when(jwtUtil.getUsername("good")).thenReturn("admin");
        when(permissionService.loadPermissions("admin")).thenReturn(List.of("JOB_VIEW", "USER_MANAGE"));

        Authentication auth = doFilter("Bearer good");
        assertNotNull(auth);
        assertEquals("admin", auth.getName());
        assertEquals(2, auth.getAuthorities().size());
        assertEquals(new SimpleGrantedAuthority("JOB_VIEW"), auth.getAuthorities().iterator().next());
    }

    @Test
    void deletedUser_emptyDbPermissions_shouldNotFallBackToToken() throws Exception {
        // 用户被删除/禁用：数据库返回空权限 → 不回退 token 内嵌权限（S1 核心回归）
        when(jwtUtil.isValid("stale")).thenReturn(true);
        when(jwtUtil.getUsername("stale")).thenReturn("deleted-user");
        when(permissionService.loadPermissions("deleted-user")).thenReturn(List.of());

        Authentication auth = doFilter("Bearer stale");
        assertNotNull(auth);
        assertEquals(0, auth.getAuthorities().size(), "删除/禁用用户必须零权限，@PreAuthorize 应全部拒绝");
        // 关键断言：空权限场景绝不读取 token 内嵌权限
        verify(jwtUtil, never()).getPermissions("stale");
    }

    @Test
    void dbException_shouldDenyClosedByDefault() throws Exception {
        // P2-5：默认拒绝闭合——DB 加载异常时不回退 token 内嵌权限，按无权限处理
        when(jwtUtil.isValid("good")).thenReturn(true);
        when(jwtUtil.getUsername("good")).thenReturn("admin");
        when(permissionService.loadPermissions("admin")).thenThrow(new RuntimeException("db down"));

        Authentication auth = doFilter("Bearer good");
        assertNotNull(auth);
        assertEquals(0, auth.getAuthorities().size(), "DB 异常默认不回退 token 权限");
        verify(jwtUtil, never()).getPermissions("good");
    }

    @Test
    void dbException_shouldFallBackToTokenPermissions_whenEnabled() throws Exception {
        // 显式开启 rxas400.security.permission-fallback-on-error=true 时才回退（可用性优先场景）
        proxyProperties.setPermissionFallbackOnError(true);
        when(jwtUtil.isValid("good")).thenReturn(true);
        when(jwtUtil.getUsername("good")).thenReturn("admin");
        when(permissionService.loadPermissions("admin")).thenThrow(new RuntimeException("db down"));
        when(jwtUtil.getPermissions("good")).thenReturn(List.of("JOB_VIEW"));

        Authentication auth = doFilter("Bearer good");
        assertNotNull(auth);
        assertEquals(1, auth.getAuthorities().size());
        assertEquals(new SimpleGrantedAuthority("JOB_VIEW"), auth.getAuthorities().iterator().next());
    }
}
