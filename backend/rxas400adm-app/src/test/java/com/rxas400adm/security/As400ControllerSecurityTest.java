package com.rxas400adm.security;

import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.controller.As400Controller;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.service.IIbmiSystemService;
import com.rxas400adm.security.config.SecurityConfig;
import com.rxas400adm.security.filter.JwtAuthenticationFilter;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.PermissionService;
import com.rxas400adm.system.aspect.OperateLogAspect;
import com.rxas400adm.system.entity.AuditLog;
import com.rxas400adm.system.mapper.AuditLogMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As400Controller 安全集成测试：服务器管理（AS400_MANAGE）门控 + @OperateLog 审计落库。
 * GET /systems 仅要求登录（未标权限码，供普通用户浏览资产清单），写操作全部 AS400_MANAGE。
 */
@WebMvcTest(controllers = As400Controller.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OperateLogAspect.class, TestAopConfig.class})
@DisplayName("As400Controller 权限门控与审计")
class As400ControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private PermissionService permissionService;
    @MockBean
    private com.rxas400adm.security.service.TokenBlacklistService tokenBlacklistService;
    @MockBean
    private IIbmiSystemService systemService;
    @MockBean
    private AuditLogMapper auditLogMapper;

    @Test
    @DisplayName("未登录访问服务器列表 → 401")
    void systems_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/as400/systems"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("任意已登录用户可读服务器列表（资产清单浏览）")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void systems_anyAuthenticated_returns200() throws Exception {
        when(systemService.list()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/as400/systems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("新增服务器：无 AS400_MANAGE → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void createSystem_withoutAs400Manage_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/as400/systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"NEW01\",\"host\":\"10.0.0.1\",\"port\":8470,\"username\":\"TESTUSER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("新增服务器：有 AS400_MANAGE → 200")
    @WithMockUser(username = "admin", authorities = "AS400_MANAGE")
    void createSystem_withAs400Manage_returns200() throws Exception {
        IbmiSystem created = new IbmiSystem();
        created.setId(9L);
        created.setName("NEW01");
        when(systemService.create(any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/as400/systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"NEW01\",\"host\":\"10.0.0.1\",\"port\":8470,\"username\":\"TESTUSER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(9));
    }

    @Test
    @DisplayName("执行 CL 命令：有权限且 @OperateLog 落库")
    @WithMockUser(username = "admin", authorities = "AS400_MANAGE")
    void command_writesOperateLog() throws Exception {
        when(systemService.executeCommand(1L, "WRKACTJOB"))
                .thenReturn(CommandResult.ok("执行成功"));

        mockMvc.perform(post("/api/v1/as400/systems/1/command")
                        .param("command", "WRKACTJOB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertEquals("AS400 管理", captor.getValue().getModule());
        assertEquals("执行 CL 命令", captor.getValue().getAction());
        assertEquals("admin", captor.getValue().getUserName());
    }

    @Test
    @DisplayName("执行 CL 命令：无权限被拦截且不写审计")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void command_withoutPermission_forbiddenAndNoAudit() throws Exception {
        mockMvc.perform(post("/api/v1/as400/systems/1/command")
                        .param("command", "WRKACTJOB"))
                .andExpect(status().isForbidden());
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }
}
