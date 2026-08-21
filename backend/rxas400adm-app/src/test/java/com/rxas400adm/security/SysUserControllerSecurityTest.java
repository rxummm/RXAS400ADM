package com.rxas400adm.security;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.security.config.SecurityConfig;
import com.rxas400adm.security.filter.JwtAuthenticationFilter;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.PermissionService;
import com.rxas400adm.system.aspect.OperateLogAspect;
import com.rxas400adm.system.controller.SysUserController;
import com.rxas400adm.system.entity.AuditLog;
import com.rxas400adm.system.mapper.AuditLogMapper;
import com.rxas400adm.system.service.SysUserService;
import com.rxas400adm.system.service.IUserMenuService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SysUserController 安全集成测试：@PreAuthorize(USER_MANAGE) 门控 + @OperateLog 审计落库。
 */
@WebMvcTest(controllers = SysUserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OperateLogAspect.class, TestAopConfig.class})
@DisplayName("SysUserController 权限门控与审计")
class SysUserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private PermissionService permissionService;
    @MockBean
    private com.rxas400adm.security.service.TokenBlacklistService tokenBlacklistService;
    @MockBean
    private SysUserService userService;
    @MockBean
    private IUserMenuService userMenuService;
    @MockBean
    private AuditLogMapper auditLogMapper;

    @Test
    @DisplayName("未登录访问用户列表 → 401")
    void users_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已登录但无 USER_MANAGE → 403")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void users_withoutUserManage_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("有 USER_MANAGE → 200")
    @WithMockUser(username = "admin", authorities = "USER_MANAGE")
    void users_withUserManage_returns200() throws Exception {
        when(userService.page(1L, 10L, null))
                .thenReturn(new PageResult<>(0, List.of()));
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("删除用户：有权限执行且 @OperateLog 不落库（该端点未标注）")
    @WithMockUser(username = "admin", authorities = "USER_MANAGE")
    void deleteUser_allowed() throws Exception {
        mockMvc.perform(delete("/api/v1/users/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(userService).delete(3L);
    }

    @Test
    @DisplayName("菜单授权：有权限且 @OperateLog 落库")
    @WithMockUser(username = "admin", authorities = "USER_MANAGE")
    void addUserMenus_writesOperateLog() throws Exception {
        mockMvc.perform(post("/api/v1/users/1/menus/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuIds\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertEquals("用户管理", captor.getValue().getModule());
        assertEquals("用户菜单授权", captor.getValue().getAction());
        assertEquals("admin", captor.getValue().getUserName());
    }

    @Test
    @DisplayName("菜单授权：无权限被拦截且不写审计")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void addUserMenus_withoutPermission_forbiddenAndNoAudit() throws Exception {
        mockMvc.perform(post("/api/v1/users/1/menus/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuIds\":[1]}"))
                .andExpect(status().isForbidden());
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }
}
