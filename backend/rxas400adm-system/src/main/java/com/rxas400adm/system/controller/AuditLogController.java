package com.rxas400adm.system.controller;

import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.service.IAuditLogService;
import com.rxas400adm.system.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;



/**
 * 审计日志中心（3.8）：平台操作/登录审计查询，按模块/用户/操作/时间过滤。
 * （IBM i 侧 SECAUD 审计属于企业级扩展，见 Phase 3 规划。）
 * R1 分层清零：Mapper/QueryWrapper 已下沉至 IAuditLogService。
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "审计日志")
public class AuditLogController {

    private final IAuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public ApiResponse<PageResult<AuditLogVO>> list(@RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "20") long size,
                                                   @RequestParam(required = false) String module,
                                                   @RequestParam(required = false) String username,
                                                   @RequestParam(required = false) String action,
                                                   @RequestParam(required = false) String keyword) {
        return ApiResponse.success(auditLogService.page(current, size, module, username, action, keyword));
    }
}
