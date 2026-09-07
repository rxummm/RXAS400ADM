package com.rxas400adm.security.service;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.vo.ProfileVO;
import com.rxas400adm.security.vo.TokenRefreshVO;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.service.SysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private IPermissionService permissionService;
    @Mock
    private ITokenBlacklistService tokenBlacklistService;
    @Mock
    private SysUserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(jwtUtil, permissionService, tokenBlacklistService, userService, passwordEncoder);
    }

    @Test
    @DisplayName("refreshToken → 无效 token 抛异常")
    void refreshToken_invalidToken_shouldThrow() {
        when(jwtUtil.isValid("bad")).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.refreshToken("bad"));
    }

    @Test
    @DisplayName("refreshToken → 非 refresh 类型 token 抛异常")
    void refreshToken_notRefreshType_shouldThrow() {
        when(jwtUtil.isValid("access-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("access-token")).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.refreshToken("access-token"));
    }

    @Test
    @DisplayName("refreshToken → 已轮换的 token（原子消费失败）抛异常")
    void refreshToken_blacklisted_shouldThrow() {
        when(jwtUtil.isValid("rt")).thenReturn(true);
        when(jwtUtil.isRefreshToken("rt")).thenReturn(true);
        when(jwtUtil.getJti("rt")).thenReturn("jti-1");
        when(jwtUtil.getUsername("rt")).thenReturn("admin");
        when(jwtUtil.getRemainingMs("rt")).thenReturn(3600000L);
        // CR-001 修复：consumeRefreshToken 返回 false（已被并发消费）
        when(tokenBlacklistService.consumeRefreshToken("jti-1", "admin", 3600000L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.refreshToken("rt"));
    }

    @Test
    @DisplayName("refreshToken → 正常路径：原子消费成功 + 签发新对")
    void refreshToken_valid_shouldRotateAndReturn() {
        when(jwtUtil.isValid("rt")).thenReturn(true);
        when(jwtUtil.isRefreshToken("rt")).thenReturn(true);
        when(jwtUtil.getJti("rt")).thenReturn("jti-1");
        when(jwtUtil.getUsername("rt")).thenReturn("admin");
        when(jwtUtil.getRemainingMs("rt")).thenReturn(3600000L);
        // CR-001 修复：consumeRefreshToken 返回 true（原子消费成功）
        when(tokenBlacklistService.consumeRefreshToken("jti-1", "admin", 3600000L)).thenReturn(true);
        // SEC-001 修复：mock 用户状态检查
        SysUser activeUser = new SysUser();
        activeUser.setUsername("admin");
        activeUser.setStatus("ACTIVE");
        when(userService.getByUsername("admin")).thenReturn(activeUser);
        when(permissionService.loadPermissions("admin")).thenReturn(List.of("JOB_VIEW"));
        when(jwtUtil.generateToken(eq("admin"), anyList())).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken("admin")).thenReturn("new-refresh");
        when(jwtUtil.getExpireMs()).thenReturn(7200000L);

        TokenRefreshVO result = service.refreshToken("rt");

        assertEquals("new-access", result.token());
        assertEquals("new-refresh", result.refreshToken());
        // CR-001 修复：验证原子消费被调用（替代原来的 blacklist）
        verify(tokenBlacklistService).consumeRefreshToken("jti-1", "admin", 3600000L);
    }

    @Test
    @DisplayName("getProfile → 用户存在返回信息")
    void getProfile_userExists_shouldReturn() {
        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setEmail("admin@test.com");
        when(userService.getByUsername("admin")).thenReturn(user);
        when(permissionService.loadPermissions("admin")).thenReturn(List.of("JOB_VIEW"));

        ProfileVO profile = service.getProfile("admin");
        assertEquals("admin", profile.username());
        assertEquals("admin@test.com", profile.email());
        assertEquals(1, profile.permissions().size());
    }

    @Test
    @DisplayName("getProfile → 用户不存在 email 为 null")
    void getProfile_userNull_shouldHandleGracefully() {
        when(userService.getByUsername("ghost")).thenReturn(null);
        when(permissionService.loadPermissions("ghost")).thenReturn(List.of());

        ProfileVO profile = service.getProfile("ghost");
        assertEquals("ghost", profile.username());
        assertNull(profile.email());
        assertTrue(profile.permissions().isEmpty());
    }
}
