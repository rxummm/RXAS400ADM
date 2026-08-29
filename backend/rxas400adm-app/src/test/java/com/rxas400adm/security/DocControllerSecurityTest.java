package com.rxas400adm.security;

import com.rxas400adm.as400.controller.DocController;
import com.rxas400adm.as400.dto.DocDTO;
import com.rxas400adm.as400.entity.Doc;
import com.rxas400adm.as400.service.DocService;
import com.rxas400adm.as400.service.DocVersionService;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.security.config.SecurityConfig;
import com.rxas400adm.security.filter.JwtAuthenticationFilter;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.PermissionService;
import com.rxas400adm.security.service.TokenBlacklistService;
import com.rxas400adm.system.aspect.OperateLogAspect;
import com.rxas400adm.system.entity.AuditLog;
import com.rxas400adm.system.mapper.AuditLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.rxas400adm.common.response.PageResult;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DocController 安全测试：文档管理（DOC_VIEW / DOC_MANAGE / DOC_APPROVE）门控 + @OperateLog。
 */
@WebMvcTest(controllers = DocController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OperateLogAspect.class, TestAopConfig.class})
@DisplayName("DocController 权限门控与审计")
class DocControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtUtil jwtUtil;
    @MockBean private PermissionService permissionService;
    @MockBean private TokenBlacklistService tokenBlacklistService;
    @MockBean private DocService docService;
    @MockBean private DocVersionService docVersionService;
    @MockBean private AuditLogMapper auditLogMapper;
    @MockBean private ProfileResolver profileResolver;

    @Test
    @DisplayName("未登录 → 401")
    void docs_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已登录用户可读文档列表（DOC_VIEW）")
    @WithMockUser(username = "admin", authorities = "DOC_VIEW")
    void docs_withDocView_returns200() throws Exception {
        when(docService.listDocs(any(), any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), anyBoolean()))
                .thenReturn(new PageResult<>(0L, List.of()));

        mockMvc.perform(get("/api/v1/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("新增文档：有 DOC_MANAGE → 200 + @OperateLog 审计")
    @WithMockUser(username = "admin", authorities = "DOC_MANAGE")
    void createDoc_withDocManage_returns200() throws Exception {
        Doc created = new Doc();
        created.setId(1L);
        when(docService.createDoc(any(DocDTO.class), any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/docs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":1,\"title\":\"变更单\",\"content\":\"正文\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogMapper).insert(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("文档管理", captor.getValue().getModule());
        org.junit.jupiter.api.Assertions.assertEquals("admin", captor.getValue().getUserName());
    }

    @Test
    @DisplayName("审批文档：有 DOC_APPROVE → 200")
    @WithMockUser(username = "approver", authorities = "DOC_APPROVE")
    void approveDoc_withDocApprove_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/docs/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
