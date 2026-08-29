package com.rxas400adm.security;

import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.security.config.SecurityConfig;
import com.rxas400adm.security.filter.JwtAuthenticationFilter;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.PermissionService;
import com.rxas400adm.security.service.TokenBlacklistService;
import com.rxas400adm.system.aspect.OperateLogAspect;
import com.rxas400adm.system.controller.ConfigController;
import com.rxas400adm.system.mapper.AuditLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConfigController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OperateLogAspect.class, TestAopConfig.class})
@DisplayName("ConfigController 权限门控")
class ConfigControllerSecurityTest {

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

    @Test
    @DisplayName("未登录 → 401")
    void noToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/configs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("无权限 → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void noPermission_403() throws Exception {
        mockMvc.perform(get("/api/v1/configs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("有 SYS_CONFIG_MANAGE 权限 → 200")
    @WithMockUser(username = "admin", authorities = "SYS_CONFIG_MANAGE")
    void list_hasPermission_200() throws Exception {
        mockMvc.perform(get("/api/v1/configs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT 无权限 → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void update_noPermission_403() throws Exception {
        mockMvc.perform(put("/api/v1/configs/testKey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"newVal\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE 无权限 → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void delete_noPermission_403() throws Exception {
        mockMvc.perform(delete("/api/v1/configs/testKey"))
                .andExpect(status().isForbidden());
    }
}