package com.rxas400adm.security.service;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.security.dto.LoginRequest;
import com.rxas400adm.security.dto.LoginResponse;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.vo.ProfileVO;
import com.rxas400adm.security.vo.TokenRefreshVO;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 认证业务服务（从 AuthController 下沉）：Token 刷新、用户信息查询。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final IPermissionService permissionService;
    private final ITokenBlacklistService tokenBlacklistService;
    private final SysUserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 平台登录：校验用户名/密码 + 用户状态，返回 token 对。
     * 失败时抛 BusinessException（LOGIN_FAILED / FORBIDDEN）。
     */
    public LoginResponse login(LoginRequest request, String ip) {
        SysUser user = userService.getByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "Invalid username or password");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "User is disabled");
        }
        List<String> permissions = permissionService.refresh(user);
        String token = jwtUtil.generateToken(user.getUsername(), permissions);
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        return new LoginResponse(token, refreshToken, jwtUtil.getExpireMs(), user.getUsername(), permissions);
    }

    /**
     * 刷新 Token：校验 refresh token → 吊销旧 refresh token（rotation）→ 签发新 token 对。
     */
    public TokenRefreshVO refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken) || !jwtUtil.isValid(refreshToken)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "Invalid refresh token");
        }
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid token type, refresh token required");
        }
        // CR-001 修复：原子消费 refresh token——INSERT 成功才允许签发新 token，
        // DuplicateKey 说明已被并发请求消费，必须拒绝（消除 TOCTOU 竞态窗口）
        String jti = jwtUtil.getJti(refreshToken);
        boolean consumed = tokenBlacklistService.consumeRefreshToken(
                jti, jwtUtil.getUsername(refreshToken), jwtUtil.getRemainingMs(refreshToken));
        if (!consumed) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "Refresh token already consumed (rotation)");
        }
        String username = jwtUtil.getUsername(refreshToken);
        // SEC-001 修复：refresh 时检查用户状态，禁用用户不允许签发新 token
        SysUser user = userService.getByUsername(username);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "User account is disabled or not found");
        }
        List<String> permissions = permissionService.loadPermissions(username);
        String newToken = jwtUtil.generateToken(username, permissions);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);
        return new TokenRefreshVO(newToken, newRefreshToken, jwtUtil.getExpireMs());
    }

    /**
     * 修改当前用户密码：委托 SysUserService 实现。
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        userService.changePassword(username, oldPassword, newPassword);
    }

    /**
     * 获取当前用户信息（username + email + permissions）。
     */
    public ProfileVO getProfile(String username) {
        SysUser user = userService.getByUsername(username);
        List<String> permissions = permissionService.loadPermissions(username);
        return new ProfileVO(username, user == null ? null : user.getEmail(), permissions);
    }
}
