package com.rxas400adm.security;

import com.rxas400adm.compile.controller.CompileController;
import com.rxas400adm.compile.dto.CompileRequest;
import com.rxas400adm.compile.entity.CompileRecord;
import com.rxas400adm.compile.service.CompileService;
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
 * CompileController 安全集成测试：@PreAuthorize(COMPILE_EXECUTE) 门控 + @OperateLog 审计落库。
 */
@WebMvcTest(controllers = CompileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OperateLogAspect.class, TestAopConfig.class})
@DisplayName("CompileController 权限门控与审计")
class CompileControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private PermissionService permissionService;
    @MockBean
    private com.rxas400adm.security.service.TokenBlacklistService tokenBlacklistService;
    @MockBean
    private CompileService compileService;
    @MockBean
    private AuditLogMapper auditLogMapper;

    @Test
    @DisplayName("未登录编译 → 401")
    void compile_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/compile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"library\":\"APP\",\"sourceFile\":\"QRPGLESRC\",\"member\":\"PGM1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已登录但无 COMPILE_EXECUTE → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void compile_withoutCompileExecute_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/compile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"library\":\"APP\",\"sourceFile\":\"QRPGLESRC\",\"member\":\"PGM1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("有 COMPILE_EXECUTE → 200 且 @OperateLog 落库")
    @WithMockUser(username = "dev01", authorities = "COMPILE_EXECUTE")
    void compile_withCompileExecute_writesOperateLog() throws Exception {
        CompileRecord record = new CompileRecord();
        record.setId(5L);
        record.setMember("PGM1");
        when(compileService.compile(any(CompileRequest.class))).thenReturn(record);

        mockMvc.perform(post("/api/v1/compile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"library\":\"APP\",\"sourceFile\":\"QRPGLESRC\",\"member\":\"PGM1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.member").value("PGM1"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertEquals("编译中心", captor.getValue().getModule());
        assertEquals("编译成员", captor.getValue().getAction());
        assertEquals("dev01", captor.getValue().getUserName());
    }

    @Test
    @DisplayName("编译历史：无 COMPILE_EXECUTE → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void history_withoutCompileExecute_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/compile/history"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("编译历史：有 COMPILE_EXECUTE → 200")
    @WithMockUser(username = "dev01", authorities = "COMPILE_EXECUTE")
    void history_withCompileExecute_returns200() throws Exception {
        when(compileService.history()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/compile/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("编译失败（服务异常）：无权限不写审计")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void compile_forbidden_noAuditWritten() throws Exception {
        mockMvc.perform(post("/api/v1/compile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"library\":\"APP\",\"sourceFile\":\"QRPGLESRC\",\"member\":\"PGM1\"}"))
                .andExpect(status().isForbidden());
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }
}
