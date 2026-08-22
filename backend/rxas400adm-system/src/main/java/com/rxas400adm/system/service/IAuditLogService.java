package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.vo.AuditLogVO;

/**
 * 审计日志服务接口（rx_audit_log）：管理端分页查询 + 登录审计写入。
 */
public interface IAuditLogService {

    /** 管理端分页查询（模块/用户/操作/关键字过滤，按创建时间倒序） */
    PageResult<AuditLogVO> page(long current, long size, String module, String username,
                                String action, String keyword);

    /** 登录审计写入（失败不影响登录主流程，内部吞异常） */
    void auditLogin(String action, String username, String ip, String source,
                    Long serverId, String detail);
}
