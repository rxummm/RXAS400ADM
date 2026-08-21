package com.rxas400adm.security.service;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.security.vo.ProfileVO;
import com.rxas400adm.security.vo.TokenRefreshVO;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
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

    /**
     * 刷新 Token：校验 refresh token → 吊销旧 refresh token（rotation）→ 签发新 token 对。
     */
    public TokenRefreshVO refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken) || !jwtUtil.isValid(refreshToken)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "无效的 refresh token");
        }
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "token 类型错误，需要 refresh token");
        }
        // 吊销旧 refresh token（rotation 防重放）
        tokenBlacklistService.blacklist(jwtUtil.getJti(refreshToken), jwtUtil.getUsername(refreshToken),
                jwtUtil.getRemainingMs(refreshToken));
        String username = jwtUtil.getUsername(refreshToken);
        List<String> permissions = permissionService.loadPermissions(username);
        String newToken = jwtUtil.generateToken(username, permissions);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);
        return new TokenRefreshVO(newToken, newRefreshToken, jwtUtil.getExpireMs());
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
