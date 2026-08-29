package com.rxas400adm.security;

import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.security.config.SecurityConfig;
import com.rxas400adm.security.filter.JwtAuthenticationFilter;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.PermissionService;
import com.rxas400adm.security.service.TokenBlacklistService;
import com.rxas400adm.system.aspect.OperateLogAspect;
import com.rxas400adm.system.controller.RegionController;
import com.rxas400adm.system.mapper.AuditLogMapper;
import com.rxas400adm.system.service.IRegionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RegionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OperateLogAspect.class, TestAopConfig.class})
@DisplayName("RegionController 权限门控")
class RegionControllerSecurityTest {

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
    private IRegionService regionService;

    @Test
    @DisplayName("未登录 → 401")
    void noToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/regions/children"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("无权限 → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void children_noPermission_403() throws Exception {
        mockMvc.perform(get("/api/v1/regions/children"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("有 REGION_VIEW 权限 → 200")
    @WithMockUser(username = "viewer", authorities = "REGION_VIEW")
    void children_hasPermission_200() throws Exception {
        when(regionService.children(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/regions/children"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("REGION_VIEW 用户访问 page → 200")
    @WithMockUser(username = "viewer", authorities = "REGION_VIEW")
    void page_hasPermission_200() throws Exception {
        when(regionService.page(1, 15, null, null, null))
                .thenReturn(new PageResult<>(0L, List.of()));

        mockMvc.perform(get("/api/v1/regions/page"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("REGION_VIEW 用户访问 search → 200")
    @WithMockUser(username = "viewer", authorities = "REGION_VIEW")
    void search_hasPermission_200() throws Exception {
        when(regionService.search(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/regions/search"))
                .andExpect(status().isOk());
    }
}