package com.rxas400adm.security;

import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.security.config.SecurityConfig;
import com.rxas400adm.security.filter.JwtAuthenticationFilter;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.PermissionService;
import com.rxas400adm.security.service.TokenBlacklistService;
import com.rxas400adm.system.aspect.OperateLogAspect;
import com.rxas400adm.system.controller.AuditLogController;
import com.rxas400adm.system.mapper.AuditLogMapper;
import com.rxas400adm.system.service.IAuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditLogController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OperateLogAspect.class, TestAopConfig.class})
@DisplayName("AuditLogController 权限门控")
class AuditLogControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileResolver profileResolver;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private PermissionService permissionService;
    @MockBean
    private TokenBlacklistService tokenBlacklistService;
    @MockBean
    private AuditLogMapper auditLogMapper;
    @MockBean
    private IAuditLogService auditLogService;

    @Test
    @DisplayName("未登录 → 401")
    void noToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("无权限 → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void noPermission_403() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("有 AUDIT_VIEW 权限 → 200")
    @WithMockUser(username = "admin", authorities = "AUDIT_VIEW")
    void hasPermission_200() throws Exception {
        when(auditLogService.page(1L, 20L, null, null, null, null))
                .thenReturn(new PageResult<>(0L, List.of()));

        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk());
    }
}