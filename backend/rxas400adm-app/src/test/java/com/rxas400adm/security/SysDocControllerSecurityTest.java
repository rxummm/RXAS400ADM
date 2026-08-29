package com.rxas400adm.security;

import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.security.config.SecurityConfig;
import com.rxas400adm.security.filter.JwtAuthenticationFilter;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.PermissionService;
import com.rxas400adm.security.service.TokenBlacklistService;
import com.rxas400adm.system.aspect.OperateLogAspect;
import com.rxas400adm.system.controller.SysDocController;
import com.rxas400adm.system.dto.SysDocDTO;
import com.rxas400adm.system.entity.AuditLog;
import com.rxas400adm.system.mapper.AuditLogMapper;
import com.rxas400adm.system.service.ISysDocService;
import com.rxas400adm.system.vo.SysDocVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SysDocController 安全测试：知识库（SYS_DOC_VIEW / SYS_DOC_MANAGE）门控 + @OperateLog。
 */
@WebMvcTest(controllers = SysDocController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OperateLogAspect.class, TestAopConfig.class})
@DisplayName("SysDocController 权限门控与审计")
class SysDocControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtUtil jwtUtil;
    @MockBean private PermissionService permissionService;
    @MockBean private TokenBlacklistService tokenBlacklistService;
    @MockBean private ISysDocService sysDocService;
    @MockBean private AuditLogMapper auditLogMapper;
    @MockBean private ProfileResolver profileResolver;

    @Test
    @DisplayName("未登录 → 401")
    void sysDocs_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/sys-docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("有 SYS_DOC_VIEW → 200")
    @WithMockUser(username = "admin", authorities = "SYS_DOC_VIEW")
    void sysDocs_withSysDocView_returns200() throws Exception {
        when(sysDocService.list(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(new PageResult<>(0L, List.of()));

        mockMvc.perform(get("/api/v1/sys-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("新建知识库：有 SYS_DOC_MANAGE → 200 + @OperateLog")
    @WithMockUser(username = "admin", authorities = "SYS_DOC_MANAGE")
    void createSysDoc_withPerm_returns200() throws Exception {
        when(sysDocService.create(any(SysDocDTO.class), eq("admin")))
                .thenReturn(new SysDocVO(1L, "运维手册", "# 内容", null, null, "DRAFT", "admin", null, null));

        mockMvc.perform(post("/api/v1/sys-docs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"运维手册\",\"content\":\"# 内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertEquals("知识库", captor.getValue().getModule());
        assertEquals("admin", captor.getValue().getUserName());
    }

    @Test
    @DisplayName("删除知识库：有权限 → 200")
    @WithMockUser(username = "admin", authorities = "SYS_DOC_MANAGE")
    void deleteSysDoc_withPerm_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/sys-docs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
