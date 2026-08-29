package com.rxas400adm.security;

import com.rxas400adm.as400.controller.BpcsOrderController;
import com.rxas400adm.as400.service.IBpcsOrderService;
import com.rxas400adm.common.config.ProfileResolver;
import com.rxas400adm.security.config.SecurityConfig;
import com.rxas400adm.security.filter.JwtAuthenticationFilter;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.PermissionService;
import com.rxas400adm.security.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BPCS Controller 安全集成测试：覆盖 BpcsOrderController（BPCS_ORDER_VIEW）门控。
 * 所有 BPCS 端点均为只读 GET，零写操作，无需审计落库验证。
 */
@WebMvcTest(controllers = BpcsOrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, TestAopConfig.class})
@DisplayName("BPCS Controller 权限门控")
class BpcsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private PermissionService permissionService;
    @MockBean
    private TokenBlacklistService tokenBlacklistService;
    @MockBean
    private ProfileResolver profileResolver;
    @MockBean
    private IBpcsOrderService bpcsOrderService;

    @Test
    @DisplayName("未登录访问 BPCS 订单头 → 401")
    void header_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/bpcs/orders/header"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已登录但无 BPCS_ORDER_VIEW → 403（订单头）")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void header_withoutBpcsOrderView_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/bpcs/orders/header")
                        .param("cono", "1")
                        .param("orno", "100001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("有 BPCS_ORDER_VIEW → 200（订单头）")
    @WithMockUser(username = "admin", authorities = "BPCS_ORDER_VIEW")
    void header_withBpcsOrderView_returns200() throws Exception {
        when(bpcsOrderService.getHeader(org.mockito.ArgumentMatchers.any()))
                .thenReturn(null);
        mockMvc.perform(get("/api/v1/bpcs/orders/header")
                        .param("cono", "1")
                        .param("orno", "100001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("未登录访问 BPCS 订单行 → 401")
    void lines_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/bpcs/orders/lines"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("已登录但无 BPCS_ORDER_VIEW → 403（订单行）")
    @WithMockUser(username = "viewer", authorities = "JOB_VIEW")
    void lines_withoutBpcsOrderView_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/bpcs/orders/lines")
                        .param("cono", "1")
                        .param("orno", "100001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("有 BPCS_ORDER_VIEW → 200（订单行）")
    @WithMockUser(username = "admin", authorities = "BPCS_ORDER_VIEW")
    void lines_withBpcsOrderView_returns200() throws Exception {
        when(bpcsOrderService.getLines(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.List.of());
        mockMvc.perform(get("/api/v1/bpcs/orders/lines")
                        .param("cono", "1")
                        .param("orno", "100001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("不同权限码不能交叉访问（BPCS_CUSTOMER_VIEW 不能访问订单）")
    @WithMockUser(username = "viewer", authorities = "BPCS_CUSTOMER_VIEW")
    void header_withWrongPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/bpcs/orders/header")
                        .param("cono", "1")
                        .param("orno", "100001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN 角色无 BPCS 权限码 → 403")
    @WithMockUser(username = "admin", authorities = "ADMIN")
    void header_withAdminOnly_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/bpcs/orders/header")
                        .param("cono", "1")
                        .param("orno", "100001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("空 authorities → 403")
    @WithMockUser(username = "noperm")
    void header_withNoAuthorities_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/bpcs/orders/header")
                        .param("cono", "1")
                        .param("orno", "100001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("不存在的 BPCS 路径 → 404（由 Spring 默认处理）")
    @WithMockUser(username = "admin", authorities = "BPCS_ORDER_VIEW")
    void nonexistentPath_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/bpcs/orders/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
