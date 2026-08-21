package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.event.UserPermissionGrantedEvent;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.UserDTO;
import com.rxas400adm.system.dto.UserUpdateDTO;
import com.rxas400adm.system.entity.SysPermission;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.entity.SysRolePermission;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.entity.SysUserRole;
import com.rxas400adm.system.mapper.SysPermissionMapper;
import com.rxas400adm.system.mapper.SysRoleMapper;
import com.rxas400adm.system.mapper.SysRolePermissionMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import com.rxas400adm.system.mapper.SysUserRoleMapper;
import com.rxas400adm.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    public SysUser getByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    @Override
    public PageResult<UserVO> page(long current, long size, String keyword) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(SysUser::getUsername, keyword).or().like(SysUser::getEmail, keyword);
        }
        qw.orderByDesc(SysUser::getCreatedTime);
        Page<SysUser> page = userMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), qw);
        List<SysUser> records = page.getRecords();
        records.forEach(u -> u.setPassword(null));
        Map<Long, List<SysRole>> rolesByUser = loadUserRoles(records);
        List<UserVO> vos = records.stream()
                .map(u -> toVO(u, rolesByUser.getOrDefault(u.getId(), List.of())))
                .toList();
        return new PageResult<>(page.getTotal(), vos);
    }

    @Override
    
    public UserVO create(UserDTO dto) {
        if (getByUsername(dto.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在: " + dto.getUsername());
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        // P2-3：未指定密码时生成强随机密码（不再用公开的 123456 兜底）；指定则校验强度
        String rawPassword = dto.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = com.rxas400adm.common.security.PasswordPolicy.generateRandom();
        } else {
            com.rxas400adm.common.security.PasswordPolicy.validate(rawPassword);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(dto.getEmail());
        user.setStatus(dto.getStatus() == null ? "ACTIVE" : dto.getStatus());
        user.setLoginSource("PLATFORM");
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.insert(user);
        List<Long> roleIds = resolveRoleIds(dto.getRoleIds(), dto.getRoleCodes());
        bindRoles(user.getId(), roleIds);
        return toVO(user, rolesByIds(roleIds));
    }

    @Override
    
    public UserVO update(Long id, UserUpdateDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        // 重置密码：dto.password 非空即覆盖（P2-3 统一强度校验）
        if (StringUtils.hasText(dto.getPassword())) {
            com.rxas400adm.common.security.PasswordPolicy.validate(dto.getPassword());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        user.setEmail(dto.getEmail());
        // 启用/禁用
        if (StringUtils.hasText(dto.getStatus())) {
            user.setStatus(dto.getStatus());
        }
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.updateById(user);
        // 角色分配：roleIds/roleCodes 任一非空时整体重建
        if (dto.getRoleIds() != null || dto.getRoleCodes() != null) {
            List<Long> roleIds = resolveRoleIds(dto.getRoleIds(), dto.getRoleCodes());
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
            bindRoles(id, roleIds);
        }
        // 启用/禁用/角色变更后立即失效权限缓存（S1）：禁用即时生效，无需等 60s TTL
        eventPublisher.publishEvent(new UserPermissionGrantedEvent(user.getUsername()));
        return toVO(user, rolesByIds(resolveRoleIds(dto.getRoleIds(), dto.getRoleCodes())));
    }

    @Override
    
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return;
        }
        // 先删关联（rx_user_role 有外键 fk_user_role_user），再删用户
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        userMapper.deleteById(id);
        // S1：删除后立即失效权限缓存，旧 token 不再回退内嵌权限
        eventPublisher.publishEvent(new UserPermissionGrantedEvent(user.getUsername()));
    }

    /**
     * RBAC 权限查询：用户 → rx_user_role → 角色 → rx_role_permission → 权限码
     */
    @Override
    
    public void updatePassword(String username, String encodedPassword) {
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .set(SysUser::getPassword, encodedPassword)
                .set(SysUser::getUpdatedTime, LocalDateTime.now()));
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {
        com.rxas400adm.common.security.PasswordPolicy.validate(newPassword);
        SysUser user = getByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "旧密码不正确");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码不能与旧密码相同");
        }
        updatePassword(username, passwordEncoder.encode(newPassword));
    }

    @Override
    public List<String> listPermissions(Long userId) {        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds))
                .stream().map(SysRolePermission::getPermissionId).distinct().collect(Collectors.toList());
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectBatchIds(permissionIds).stream()
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toList());
    }

    // ---------- helpers ----------

    /**
     * 角色解析：优先 roleIds；为空时按 roleCodes（角色代码）查 rx_role 解析为 ID。
     * 代码传了但查不到对应角色时抛错，避免静默绑定失败。
     */
    private List<Long> resolveRoleIds(List<Long> roleIds, List<String> roleCodes) {
        if (roleIds != null && !roleIds.isEmpty()) {
            return roleIds;
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        List<String> codes = roleCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(c -> !c.isBlank())
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            return List.of();
        }
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getRoleCode, codes));
        List<String> found = roles.stream().map(SysRole::getRoleCode).toList();
        List<String> missing = codes.stream().filter(c -> !found.contains(c)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "角色不存在: " + String.join(", ", missing));
        }
        return roles.stream().map(SysRole::getId).toList();
    }

    private void bindRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<Long> distinctIds = roleIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, SysRole> existingRoles = roleMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(SysRole::getId, r -> r));
        List<Long> missing = distinctIds.stream().filter(id -> !existingRoles.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "角色不存在: " + missing.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        }
        List<SysUserRole> userRoles = distinctIds.stream().map(roleId -> {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            return ur;
        }).toList();
        userRoleMapper.insertBatch(userRoles);
    }

    /** 批量查询一批用户的角色，按 userId 分组 */
    private Map<Long, List<SysRole>> loadUserRoles(List<SysUser> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
        if (userRoles.isEmpty()) {
            return Map.of();
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).distinct().toList();
        Map<Long, SysRole> roleMap = roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, r -> r));
        return userRoles.stream().collect(Collectors.groupingBy(
                SysUserRole::getUserId,
                Collectors.mapping(ur -> roleMap.get(ur.getRoleId()), Collectors.toList())));
    }

    private List<SysRole> rolesByIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds.stream().filter(Objects::nonNull).distinct().toList());
    }

    private UserVO toVO(SysUser user, List<SysRole> roles) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus())
                .loginSource(user.getLoginSource())
                .as400ServerId(user.getAs400ServerId())
                .roles(roles == null ? Collections.emptyList() : roles)
                .createdTime(user.getCreatedTime())
                .build();
    }
}