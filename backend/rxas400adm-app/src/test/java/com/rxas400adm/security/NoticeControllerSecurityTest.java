package com.rxas400adm.security;

import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.security.config.SecurityConfig;
import com.rxas400adm.security.filter.JwtAuthenticationFilter;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.PermissionService;
import com.rxas400adm.security.service.TokenBlacklistService;
import com.rxas400adm.system.aspect.OperateLogAspect;
import com.rxas400adm.system.controller.NoticeController;
import com.rxas400adm.system.mapper.AuditLogMapper;
import com.rxas400adm.system.service.INoticeService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NoticeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OperateLogAspect.class, TestAopConfig.class})
@DisplayName("NoticeController 权限门控")
class NoticeControllerSecurityTest {

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
    private INoticeService noticeService;

    @Test
    @DisplayName("未登录 → 401")
    void noToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/notices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("任意已登录用户可读已发布公告（无需权限）")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void published_anyAuthenticated_200() throws Exception {
        when(noticeService.published()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notices"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("无 NOTICE_MANAGE 权限访问 page → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void page_noPermission_403() throws Exception {
        mockMvc.perform(get("/api/v1/notices/page"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("有 NOTICE_MANAGE 权限访问 page → 200")
    @WithMockUser(username = "admin", authorities = "NOTICE_MANAGE")
    void page_hasPermission_200() throws Exception {
        when(noticeService.page(1, 10, null, null))
                .thenReturn(new PageResult<>(0L, List.of()));

        mockMvc.perform(get("/api/v1/notices/page"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST 无权限 → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void create_noPermission_403() throws Exception {
        mockMvc.perform(post("/api/v1/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"test\",\"content\":\"body\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE 无权限 → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void delete_noPermission_403() throws Exception {
        mockMvc.perform(delete("/api/v1/notices/1"))
                .andExpect(status().isForbidden());
    }
}
