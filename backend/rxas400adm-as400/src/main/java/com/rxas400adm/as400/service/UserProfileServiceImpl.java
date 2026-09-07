package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400Client;
import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.sql.SqlStatementRegistry;
import com.rxas400adm.as400.dto.UserProfileCreateDTO;
import com.rxas400adm.as400.dto.UserProfileUpdateDTO;
import com.rxas400adm.as400.entity.UserProfileLog;
import com.rxas400adm.as400.mapper.UserProfileLogMapper;
import com.rxas400adm.as400.model.UserProfileListRow;
import com.rxas400adm.as400.vo.UserProfileCreateResult;
import com.rxas400adm.as400.vo.UserProfileDetailVO;
import com.rxas400adm.common.constants.As400Identifiers;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.security.SecretMasker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * AS400用户Profile管理Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements IUserProfileService {

    private final AS400ClientProvider clientProvider;
    private final UserProfileLogMapper userProfileLogMapper;

    @Override
    public List<UserProfileListRow> listUserProfiles() {
        return clientProvider.current().listUserProfiles();
    }

    @Override
    public UserProfileDetailVO getUserProfile(String userName) {
        requireValidIdentifier(userName);
        AS400Client client = clientProvider.current();
        
        // 查询用户详情（参数化查询防SQL注入）
        String sql = SqlStatementRegistry.of("auth.user.detail");
        
        List<Map<String, Object>> results = client.queryListChecked(sql, userName.toUpperCase());
        if (results.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "User not found: " + userName);
        }
        
        Map<String, Object> row = results.get(0);
        return new UserProfileDetailVO(
                getStringValue(row, "USER_NAME"),
                getStringValue(row, "STATUS"),
                getStringValue(row, "GROUP_PROFILE"),
                getStringValue(row, "TEXT_DESCRIPTION"),
                null, // 初始菜单需要从其他地方获取
                List.of(), // 特殊权限需要从其他地方获取
                getStringValue(row, "LAST_USED_DATE"),
                getStringValue(row, "PASSWORD_EXPIRE_DATE")
        );
    }

    @Override
    public UserProfileCreateResult createUserProfile(UserProfileCreateDTO dto, String operator) {
        requireValidIdentifier(dto.getUserName());
        AS400Client client = clientProvider.current();
        
        // S-7 修复：*ALLOBJ/*SECADM 仅限 ADMIN 角色
        requireAdminForCriticalAuth(dto.getSpecialAuthorities(), operator);
        
        // 构建CRTUSRPRF命令
        String command = buildCreateCommand(dto);
        
        log.info("执行创建用户Profile命令: {}, 操作人: {}", SecretMasker.maskClCommand(command), operator);
        
        // 执行命令
        CommandResult result = client.execute(command);
        
        if (!result.success()) {
            log.error("创建用户Profile失败: {}", result.message());
            saveLog(dto.getUserName(), "CREATE", operator, "失败: " + result.message());
            return UserProfileCreateResult.fail(dto.getUserName(), result.message());
        }
        
        // 记录操作日志
        saveLog(dto.getUserName(), "CREATE", operator, 
                "创建用户成功，描述: " + dto.getDescription() + 
                ", 组Profile: " + dto.getGroupProfile());
        
        log.info("创建用户Profile成功: {}", dto.getUserName());
        return UserProfileCreateResult.success(dto.getUserName());
    }

    @Override
    public void updateUserProfile(String userName, UserProfileUpdateDTO dto, String operator) {
        requireValidIdentifier(userName);
        AS400Client client = clientProvider.current();
        
        // S-7 修复：*ALLOBJ/*SECADM 仅限 ADMIN 角色
        requireAdminForCriticalAuth(dto.getSpecialAuthorities(), operator);
        
        // 构建CHGUSRPRF命令
        String command = buildUpdateCommand(userName, dto);
        
        log.info("执行更新用户Profile命令: {}, 操作人: {}", SecretMasker.maskClCommand(command), operator);
        
        // 执行命令
        CommandResult result = client.execute(command);
        
        if (!result.success()) {
            log.error("更新用户Profile失败: {}", result.message());
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, result.message());
        }
        
        // 记录操作日志
        saveLog(userName, "UPDATE", operator, "更新用户信息");
        
        log.info("更新用户Profile成功: {}", userName);
    }

    @Override
    public void deleteUserProfile(String userName, String operator) {
        requireValidIdentifier(userName);
        AS400Client client = clientProvider.current();
        
        // 构建DLTUSRPRF命令
        String command = "DLTUSRPRF USRPRF(" + userName.toUpperCase() + ")";
        
        log.info("执行删除用户Profile命令: {}, 操作人: {}", SecretMasker.maskClCommand(command), operator);
        
        // 执行命令
        CommandResult result = client.execute(command);
        
        if (!result.success()) {
            log.error("删除用户Profile失败: {}", result.message());
            throw new BusinessException(ErrorCode.AS400_COMMAND_FAILED, result.message());
        }
        
        // 记录操作日志
        saveLog(userName, "DELETE", operator, "删除用户");
        
        log.info("删除用户Profile成功: {}", userName);
    }

    private String buildCreateCommand(UserProfileCreateDTO dto) {
        StringBuilder cmd = new StringBuilder("CRTUSRPRF");
        cmd.append(" USRPRF(").append(dto.getUserName().toUpperCase()).append(")");
        cmd.append(" PASSWORD(").append(escapeClString(dto.getPassword())).append(")");
        
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            cmd.append(" TEXT('").append(escapeClString(dto.getDescription())).append("')");
        }
        
        if (dto.getGroupProfile() != null && !dto.getGroupProfile().isBlank()) {
            requireValidIdentifier(dto.getGroupProfile());
            cmd.append(" GRPPRF(").append(dto.getGroupProfile().toUpperCase()).append(")");
        }
        
        if (dto.getInitialMenu() != null && !dto.getInitialMenu().isBlank()) {
            requireValidIdentifier(dto.getInitialMenu());
            cmd.append(" INLMNU(").append(dto.getInitialMenu().toUpperCase()).append(")");
        }
        
        if (dto.getSpecialAuthorities() != null && !dto.getSpecialAuthorities().isEmpty()) {
            StringJoiner joiner = new StringJoiner(" ");
            dto.getSpecialAuthorities().forEach(auth -> {
                requireValidIdentifier(auth);
                joiner.add(auth.toUpperCase());
            });
            cmd.append(" SPCAUT(").append(joiner).append(")");
        }
        
        return cmd.toString();
    }

    private String buildUpdateCommand(String userName, UserProfileUpdateDTO dto) {
        StringBuilder cmd = new StringBuilder("CHGUSRPRF");
        cmd.append(" USRPRF(").append(userName.toUpperCase()).append(")");
        
        if (dto.getDescription() != null) {
            cmd.append(" TEXT('").append(escapeClString(dto.getDescription())).append("')");
        }
        
        if (dto.getGroupProfile() != null) {
            requireValidIdentifier(dto.getGroupProfile());
            cmd.append(" GRPPRF(").append(dto.getGroupProfile().toUpperCase()).append(")");
        }
        
        if (dto.getStatus() != null) {
            // AS400-014 修复：STATUS 必须是白名单值，防止任意字符串进入 CL 命令
            String statusVal = dto.getStatus().toUpperCase();
            if (!"*ENABLED".equals(statusVal) && !"*DISABLED".equals(statusVal)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Invalid STATUS value: " + statusVal + ". Allowed: *ENABLED, *DISABLED");
            }
            cmd.append(" STATUS(").append(statusVal).append(")");
        }
        
        if (dto.getInitialMenu() != null) {
            requireValidIdentifier(dto.getInitialMenu());
            cmd.append(" INLMNU(").append(dto.getInitialMenu().toUpperCase()).append(")");
        }
        
        if (dto.getSpecialAuthorities() != null && !dto.getSpecialAuthorities().isEmpty()) {
            StringJoiner joiner = new StringJoiner(" ");
            dto.getSpecialAuthorities().forEach(auth -> {
                requireValidIdentifier(auth);
                joiner.add(auth.toUpperCase());
            });
            cmd.append(" SPCAUT(").append(joiner).append(")");
        }
        
        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            cmd.append(" PASSWORD(").append(escapeClString(dto.getNewPassword())).append(")");
        }
        
        return cmd.toString();
    }

    private void requireValidIdentifier(String value) {
        if (value == null || !As400Identifiers.IDENTIFIER.matcher(value.toUpperCase()).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid IBM i identifier: " + value);
        }
    }

    /** S-7 修复：*ALLOBJ/*SECADM 仅限 ADMIN 角色操作 */
    private void requireAdminForCriticalAuth(List<String> specialAuthorities, String operator) {
        if (specialAuthorities == null || specialAuthorities.isEmpty()) return;
        boolean hasCritical = specialAuthorities.stream()
                .anyMatch(a -> "*ALLOBJ".equalsIgnoreCase(a) || "*SECADM".equalsIgnoreCase(a));
        if (hasCritical && !isCurrentUserAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Special authorities *ALLOBJ/*SECADM require ADMIN role");
        }
    }

    /** 从 Spring Security 上下文判断当前用户是否拥有 ADMIN 角色（JWT token 中已包含 ROLE_xxx 权限） */
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
    }

    /** CL 字符串转义：单引号加倍，防止 CL 命令注入 */
    private String escapeClString(String value) {
        if (value == null) return "";
        return value.replace("'", "''");
    }

    private void saveLog(String userName, String action, String operator, String detail) {
        UserProfileLog logEntry = new UserProfileLog();
        logEntry.setUserName(userName);
        logEntry.setAction(action);
        logEntry.setOperator(operator);
        logEntry.setDetail(detail);
        logEntry.setCreatedTime(LocalDateTime.now());
        userProfileLogMapper.insert(logEntry);
    }

    private String getStringValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }
}
