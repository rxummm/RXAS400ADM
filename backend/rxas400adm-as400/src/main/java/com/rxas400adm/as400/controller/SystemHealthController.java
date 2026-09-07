package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.ISystemHealthService;
import com.rxas400adm.as400.vo.SecurityAuditSummaryVO;
import com.rxas400adm.as400.vo.SystemHealthOverviewVO;
import com.rxas400adm.as400.vo.UserPermissionMatrixVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/as400/system-health")
@RequiredArgsConstructor
@Tag(name = "系统健康仪表板", description = "A1系统级综合视图 / A2安全审计 / A3权限矩阵")
public class SystemHealthController {

    private final ISystemHealthService systemHealthService;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('SYSTEM_HEALTH_VIEW')")
    public ApiResponse<SystemHealthOverviewVO> getOverview() {
        return ApiResponse.success(systemHealthService.getOverview());
    }

    @GetMapping("/security-audit")
    @PreAuthorize("hasAuthority('SYSTEM_HEALTH_VIEW')")
    public ApiResponse<List<SecurityAuditSummaryVO>> getSecurityAuditSummary() {
        return ApiResponse.success(systemHealthService.getSecurityAuditSummary());
    }

    @GetMapping("/permission-matrix")
    @PreAuthorize("hasAuthority('SYSTEM_HEALTH_VIEW')")
    public ApiResponse<List<UserPermissionMatrixVO>> getUserPermissionMatrix() {
        return ApiResponse.success(systemHealthService.getUserPermissionMatrix());
    }
}
