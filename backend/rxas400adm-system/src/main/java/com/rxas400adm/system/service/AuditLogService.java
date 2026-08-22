package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.entity.AuditLog;
import com.rxas400adm.system.mapper.AuditLogMapper;
import com.rxas400adm.system.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 审计日志服务（rx_audit_log）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService implements IAuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public PageResult<AuditLogVO> page(long current, long size, String module, String username,
                                       String action, String keyword) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            wrapper.eq(AuditLog::getModule, module.trim());
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(AuditLog::getUserName, username.trim());
        }
        if (StringUtils.hasText(action)) {
            wrapper.like(AuditLog::getAction, action.trim());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AuditLog::getDetail, keyword.trim())
                    .or().like(AuditLog::getTarget, keyword.trim()));
        }
        wrapper.orderByDesc(AuditLog::getCreatedTime);
        Page<AuditLog> page = auditLogMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(AuditLogVO::from).toList());
    }

    @Override
    public void auditLogin(String action, String username, String ip, String source,
                           Long serverId, String detail) {
        try {
            AuditLog audit = new AuditLog();
            audit.setUserName(username);
            audit.setAction(action);
            audit.setModule("登录安全");
            audit.setTarget(source + (serverId == null ? "" : " server=" + serverId));
            audit.setIp(ip);
            audit.setDetail(detail);
            audit.setCreatedTime(LocalDateTime.now());
            auditLogMapper.insert(audit);
        } catch (Exception e) {
            log.warn("登录审计写入失败: {}", e.getMessage());
        }
    }
}