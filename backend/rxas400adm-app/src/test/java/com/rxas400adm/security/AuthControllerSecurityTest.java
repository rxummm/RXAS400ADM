package com.rxas400adm.security;

import com.rxas400adm.security.config.SecurityConfig;
import com.rxas400adm.security.controller.AuthController;
import com.rxas400adm.security.filter.JwtAuthenticationFilter;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.As400LoginService;
import com.rxas400adm.security.service.AuthService;
import com.rxas400adm.security.service.IpRuleService;
import com.rxas400adm.security.service.LoginAttemptService;
import com.rxas400adm.security.service.PermissionService;
import com.rxas400adm.security.service.TokenBlacklistService;
import com.rxas400adm.system.aspect.OperateLogAspect;
import com.rxas400adm.system.entity.AuditLog;
import com.rxas400adm.system.mapper.AuditLogMapper;
import com.rxas400adm.system.service.IAuditLogService;
import com.rxas400adm.system.service.MenuService;
import com.rxas400adm.system.service.SysUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import com.rxas400adm.common.config.ProfileResolver;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 安全集成测试：验证 @PreAuthorize 权限码门控与 @OperateLog 审计落库。
 * 覆盖：未登录 401 / 无权限 403 / 有权限 200 + 审计写入。
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OperateLogAspect.class, TestAopConfig.class})
@DisplayName("AuthController 权限门控与审计")
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // 切片过滤器排除普通 @Component（GlobalExceptionHandler 依赖），显式 Mock
    @MockBean
    private ProfileResolver profileResolver;
    @MockBean
    private SysUserService userService;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private PermissionService permissionService;
    @MockBean
    private TokenBlacklistService tokenBlacklistService;
    @MockBean
    private As400LoginService as400LoginService;
    @MockBean
    private LoginAttemptService loginAttemptService;
    @MockBean
    private IpRuleService ipRuleService;
    @MockBean
    private AuditLogMapper auditLogMapper;
    @MockBean
    private IAuditLogService auditLogService;
    @MockBean
    private MenuService menuService;
    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("未登录访问受保护端点 → 401")
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login-attempts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已登录但无 USER_MANAGE → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void loginAttempts_withoutUserManage_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login-attempts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("有 USER_MANAGE → 200")
    @WithMockUser(username = "admin", authorities = "USER_MANAGE")
    void loginAttempts_withUserManage_returns200() throws Exception {
        when(loginAttemptService.listAttempts(null)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/auth/login-attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("解锁账号：有权限执行且 @OperateLog 落库")
    @WithMockUser(username = "admin", authorities = "USER_MANAGE")
    void unlock_writesOperateLog() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/login-attempts/lockedUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertEquals("登录安全", captor.getValue().getModule());
        assertEquals("手动解锁账号", captor.getValue().getAction());
        assertEquals("admin", captor.getValue().getUserName());
    }

    @Test
    @DisplayName("解锁账号：无权限时被拦截且不写审计")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void unlock_withoutPermission_forbiddenAndNoAudit() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/login-attempts/lockedUser"))
                .andExpect(status().isForbidden());
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    @Test
    @DisplayName("登录接口公开（PERMIT_ALL）：无需认证即可访问")
    void login_isPermitAll() throws Exception {
        // 无 token 也能到达 Controller（未被安全链拦截为 401/403，即证明登录接口是公开的）
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }
}
