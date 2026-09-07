package com.rxas400adm.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.model.UserProfileRow;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.security.dto.As400LoginRequest;
import com.rxas400adm.security.dto.LoginResponse;
import com.rxas400adm.security.jwt.JwtUtil;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import com.rxas400adm.system.service.ISysConfigService;
import com.rxas400adm.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AS400 user profile 登录（追踪文档 2.2）：
 * <ol>
 *   <li>凭据认证：委托 AS400Client.authenticate(user, pass)（JT400 真实校验 / mock 返回成功）</li>
 *   <li>组 → 角色映射（2.2.4）：查询该用户在 IBM i 的组 profile（AS400Client.userProfile），
 *       按 sys_config 的 as400.login.groupRoleMapping（JSON：组→角色码）分配角色，未命中用默认 VIEWER</li>
 *   <li>本地映射：大小写不敏感匹配 sys_user.username，不存在则自动创建（密码随机不可登录、
 *       login_source=AS400、as400_server_id 标记来源）；登录时按组映射做角色收敛</li>
 *   <li>发 JWT（同平台登录）</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class As400LoginService implements IAs400LoginService {

    /** AS400 登录用户的默认角色（只读），未命中组映射时使用 */
    static final String DEFAULT_ROLE = "VIEWER";
    /** 组 → 角色映射配置键（sys_config，JSON：{"GRPDEV":"DEVELOPER",...}） */
    static final String GROUP_ROLE_MAPPING_KEY = "as400.login.groupRoleMapping";
    /** 默认映射（首次启动写入 sys_config） */
    static final String DEFAULT_GROUP_ROLE_MAPPING =
            "{\"GRPADM\":\"ADMIN\",\"GRPDEV\":\"DEVELOPER\",\"GRPOPR\":\"OPERATOR\"}";

    private final AS400ClientProvider clientProvider;
    private final SysUserService userService;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final ISysConfigService sysConfigService;
    private final ObjectMapper objectMapper;
    private final IPermissionService permissionService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    
    public LoginResponse login(As400LoginRequest request) {
        AS400Client client = clientProvider.forServer(request.getServerId());
        if (!client.authenticate(request.getUsername(), request.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "Invalid AS400 credentials");
        }
        SysUser user = resolveOrCreateUser(client, request);
        // 角色收敛后强制刷新权限缓存（S1）：组映射变更立即体现在本次会话与 token 中
        List<String> permissions = permissionService.refresh(user.getUsername());
        String token = jwtUtil.generateToken(user.getUsername(), permissions);
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        return new LoginResponse(token, refreshToken, jwtUtil.getExpireMs(), user.getUsername(), permissions);
    }

    private SysUser resolveOrCreateUser(AS400Client client, As400LoginRequest request) {
        String name = request.getUsername().trim();
        List<String> roleCodes = resolveRoles(client, name);

        SysUser user = userService.getByUsername(name);
        if (user != null) {
            // S1：平台侧已禁用/删除的本地映射账号，AS400 认证通过也不允许登录
            if (!"ACTIVE".equals(user.getStatus())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "User is disabled, cannot login via AS400");
            }
            // 平台注册用户首次用 AS400 账号登录：补记来源标记
            if (!"AS400".equals(user.getLoginSource())) {
                user.setLoginSource("AS400");
                user.setAs400ServerId(request.getServerId());
                userMapper.updateById(user);
            }
            // 登录时按组映射做角色收敛（覆盖本地角色，参考旧项目）
            applyRoles(user.getId(), roleCodes);
            return user;
        }

        SysUser created = new SysUser();
        created.setUsername(name);
        created.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        created.setEmail(null);
        created.setStatus("ACTIVE");
        created.setLoginSource("AS400");
        created.setCreatedBy("AS400-SYNC");
        created.setAs400ServerId(request.getServerId());
        created.setCreatedTime(LocalDateTime.now());
        created.setUpdatedTime(LocalDateTime.now());
        userMapper.insert(created);
        applyRoles(created.getId(), roleCodes);
        log.info("AS400 login auto-created user: {} (server={}, roles={})", name, request.getServerId(), roleCodes);
        return created;
    }

    /** 按用户在 IBM i 的组 profile 解析应分配的角色码（未命中映射 → 默认 VIEWER） */
    private List<String> resolveRoles(AS400Client client, String username) {
        UserProfileRow profile = client.userProfile(username);
        String group = profile == null || profile.groupProfile() == null
                ? "" : profile.groupProfile();
        Map<String, String> mapping = loadGroupRoleMapping();
        String roleCode = mapping.getOrDefault(group, DEFAULT_ROLE);
        return List.of(roleCode);
    }

    /** 读取 sys_config 的组 → 角色映射 JSON（供每日同步复用） */
    @Override
    public Map<String, String> loadGroupRoleMapping() {
        String json = sysConfigService.get(GROUP_ROLE_MAPPING_KEY, "");
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            log.warn("组角色映射 JSON 解析失败: {}", e.getMessage());
            return Map.of();
        }
    }

    /** 重建用户角色（先删后插，供每日同步复用） */
    @Override
    public void applyRoles(Long userId, List<String> roleCodes) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        // B7：一次性按 roleCode 批量查询，消除逐条 selectOne 的 N+1
        List<SysRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getRoleCode, roleCodes));
        Map<String, Long> codeToId = roles.stream()
                .collect(Collectors.toMap(SysRole::getRoleCode, SysRole::getId, (a, b) -> b));
        // T5：单条多值 INSERT 收窄「先删后插」窗口（无事务架构下的批量化收口）
        List<SysUserRole> userRoles = new ArrayList<>();
        for (String code : roleCodes) {
            Long roleId = codeToId.get(code);
            if (roleId != null) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoles.add(ur);
            }
        }
        if (!userRoles.isEmpty()) {
            userRoleMapper.insertBatch(userRoles);
        }
    }
}