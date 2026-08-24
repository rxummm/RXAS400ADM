package com.rxas400adm.security.controller;
import com.rxas400adm.common.util.SecurityUtils;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.constants.SecurityConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.security.dto.As400LoginRequest;
import com.rxas400adm.security.dto.ChangePasswordDTO;
import com.rxas400adm.security.dto.LoginRequest;
import com.rxas400adm.security.dto.LoginResponse;
import com.rxas400adm.security.dto.RefreshTokenDTO;
import com.rxas400adm.security.vo.LoginAttemptIpStatsVO;
import com.rxas400adm.security.vo.LoginAttemptVO;
import com.rxas400adm.security.vo.MenuDataResponseVO;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.service.IAs400LoginService;
import com.rxas400adm.security.service.IIpRuleService;
import com.rxas400adm.security.service.ILoginAttemptService;
import com.rxas400adm.security.service.IPermissionService;
import com.rxas400adm.security.service.ITokenBlacklistService;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.service.IAuditLogService;
import com.rxas400adm.system.service.IMenuService;
import com.rxas400adm.system.service.SysUserService;
import com.rxas400adm.system.vo.UserMenuDataVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "认证授权", description = "登录 / 登出 / Token 刷新 / 动态菜单 / 密码修改")
@Tag(name = "认证授权")
public class AuthController {

    private final SysUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final IPermissionService permissionService;
    private final IAs400LoginService as400LoginService;
    private final ILoginAttemptService loginAttemptService;
    private final IIpRuleService ipRuleService;
    private final IAuditLogService auditLogService;
    private final IMenuService menuService;
    private final ITokenBlacklistService tokenBlacklistService;
    private final com.rxas400adm.security.service.AuthService authService;

    /** 可信反向代理 IP 列表（逗号分隔，S3）；留空则完全忽略 X-Forwarded-For */
    @Value("${rxas400.security.trusted-proxies:}")
    private String trustedProxies;

    /**
     * 登出（P2-1 JWT 吊销）：将当前 token 的 jti 加入吊销名单，
     * 有效期内该 token 无法再通过 JwtAuthenticationFilter 认证。
     * 幂等：重复登出/无 token 也返回成功（前端先清本地状态）。
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @OperateLog(module = "认证", operation = "登出")
    public ApiResponse<Void> logout(@org.springframework.web.bind.annotation.RequestHeader(
            value = org.springframework.http.HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            String token = authHeader.substring(SecurityConstants.TOKEN_PREFIX.length());
            if (jwtUtil.isValid(token)) {
                tokenBlacklistService.blacklist(jwtUtil.getJti(token), jwtUtil.getUsername(token),
                        jwtUtil.getRemainingMs(token));
            }
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        // 安全前置：IP 黑白名单 + IP 限流 + 用户名锁定检查（平台登录 serverId=null）
        ipRuleService.checkIp(ip);
        loginAttemptService.checkIpRate(ip);
        loginAttemptService.checkUsernameLock(request.getUsername(), null);

        SysUser user = userService.getByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.registerFailure(request.getUsername(), null, ip);
            auditLogin("LOGIN_FAILED", request.getUsername(), ip, "PLATFORM", null, "用户名或密码错误");
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            auditLogin("LOGIN_FAILED", request.getUsername(), ip, "PLATFORM", null, "用户已被禁用");
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户已被禁用");
        }
        loginAttemptService.clearFailure(request.getUsername(), null);
        // 登录成功刷新权限缓存（角色变更立即在新会话体现，2.5.1）
        // 传入已查询的 user 对象，避免 loadFromDb 重复 getByUsername
        List<String> permissions = permissionService.refresh(user);
        String token = jwtUtil.generateToken(user.getUsername(), permissions);
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        auditLogin("LOGIN_SUCCESS", user.getUsername(), ip, "PLATFORM", null, "平台账号登录成功");
        return ApiResponse.success(new LoginResponse(token, refreshToken, jwtUtil.getExpireMs(), user.getUsername(), permissions));
    }

    /**
     * AS400 user profile 登录：凭据委托 IBM i 认证，自动创建/映射本地用户（追踪文档 2.2.2/2.2.3）。
     * 登录安全（2.2.7/2.2.6）同样生效：IP 限流 + 失败锁定 + 审计（含服务器）。
     */
    @PostMapping("/as400-login")
    public ApiResponse<LoginResponse> as400Login(@Valid @RequestBody As400LoginRequest request,
                                                 HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        ipRuleService.checkIp(ip);
        loginAttemptService.checkIpRate(ip);
        loginAttemptService.checkUsernameLock(request.getUsername(), request.getServerId());
        try {
            LoginResponse response = as400LoginService.login(request);
            loginAttemptService.clearFailure(request.getUsername(), request.getServerId());
            auditLogin("AS400_LOGIN_SUCCESS", response.getUsername(), ip, "AS400",
                    request.getServerId(), "AS400 账号登录成功");
            return ApiResponse.success(response);
        } catch (BusinessException e) {
            loginAttemptService.registerFailure(request.getUsername(), request.getServerId(), ip);
            auditLogin("AS400_LOGIN_FAILED", request.getUsername(), ip, "AS400",
                    request.getServerId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 登录失败记录列表（按服务器维度统计，2.2.7 增强）：
     * 仅管理员可查看，便于安全事件分析与锁定排查。
     */
    @GetMapping("/login-attempts")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<List<LoginAttemptVO>> loginAttempts(@RequestParam(required = false) Long serverId) {
        return ApiResponse.success(loginAttemptService.listAttempts(serverId)
                .stream().map(LoginAttemptVO::from).toList());
    }

    /** 按 IP 聚合统计（暴力破解溯源） */
    @GetMapping("/login-attempts/ips")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<List<LoginAttemptIpStatsVO>> loginAttemptIps() {
        return ApiResponse.success(loginAttemptService.aggregateByIp().stream()
                .map(LoginAttemptIpStatsVO::from).toList());
    }

    /** 手动解锁（清除该用户名在指定服务器上的失败记录；serverId 缺省为平台） */
    @DeleteMapping("/login-attempts/{username}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @OperateLog(module = "登录安全", operation = "手动解锁账号")
    public ApiResponse<Void> unlock(@PathVariable String username,
                                    @RequestParam(required = false) Long serverId) {
        loginAttemptService.clearFailure(username, serverId);
        return ApiResponse.success(null);
    }

    /**
     * 修改当前登录用户密码（个人中心）：
     * 校验旧密码正确后加密覆盖；新密码至少 8 位。
     */
    @PostMapping("/change-password")
    @OperateLog(module = "登录安全", operation = "修改密码")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto,
                                            HttpServletRequest httpRequest) {
        String username = SecurityUtils.currentUsername();
        userService.changePassword(username, dto.getOldPassword(), dto.getNewPassword());
        auditLogin("PASSWORD_CHANGE", username, clientIp(httpRequest), "PLATFORM", null, "修改登录密码");
        return ApiResponse.success(null);
    }

    /**
     * P2: Refresh token 端点：校验 refresh token 有效性后签发新的 access token。
     * Refresh token 本身不可刷新（rotation），旧 refresh token 加入黑名单。
     */
    @PostMapping("/refresh")
    public ApiResponse<com.rxas400adm.security.vo.TokenRefreshVO> refresh(@Valid @RequestBody RefreshTokenDTO dto,
                                                    HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.refreshToken(dto.getRefreshToken()));
    }

    @GetMapping("/profile")
    public ApiResponse<com.rxas400adm.security.vo.ProfileVO> profile() {
        return ApiResponse.success(authService.getProfile(SecurityUtils.currentUsername()));
    }

    /**
     * 动态菜单 + 按钮权限码（后端下发）：菜单由 rx_menu 表驱动 + 角色授权裁剪（参照旧项目 sys_menu/sys_role_menu）。
     * - ADMIN 角色：全部启用菜单（status=1）
     * - 其他角色：仅下发 rx_role_menu 已授权菜单（含祖先目录），并排除 admin_only
     * - 菜单被隐藏（status=0）或角色未授权 → 左侧菜单不显示
     * - perms：权限表码（rx_permission）∪ 菜单码（menu_type 1/2/3 带 perms，随 rx_role_menu/rx_user_menu 授权）——
     *   供前端 hasPermission / v-has-perm 控制按钮显隐
     * title 为 i18n key（如 "dashboard"），前端通过 $t('menu.' + title) 渲染。
     */
    @GetMapping("/menu")
    public ApiResponse<MenuDataResponseVO> menu() {
        String username = SecurityUtils.currentUsername();
        UserMenuDataVO menuData = menuService.userMenuData(username);
        Set<String> perms = new LinkedHashSet<>(permissionService.loadPermissions(username));
        perms.addAll(menuData.perms());
        return ApiResponse.success(new MenuDataResponseVO(
                menuData.menus(), perms, menuData.tabs()));
    }

    private void auditLogin(String action, String username, String ip, String source,
                            Long serverId, String detail) {
        auditLogService.auditLogin(new com.rxas400adm.system.service.LoginAuditContext(
                action, username, ip, source, serverId, detail));
    }

    /**
     * S3：仅当请求直接来自可信反向代理时才信任 X-Forwarded-For，
     * 否则攻击者直连伪造 XFF 即可绕过 IP 限流/黑白名单/失败锁定。
     */
    private String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (isTrustedProxy(remote)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return remote;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || !StringUtils.hasText(trustedProxies)) {
            return false;
        }
        return Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(p -> !p.isBlank())
                .anyMatch(p -> "*".equals(p) || p.equalsIgnoreCase(remoteAddr));
    }

}
