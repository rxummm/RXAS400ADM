package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.ISystemValueComplianceService;
import com.rxas400adm.as400.vo.SystemValueComplianceVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/as400/compliance")
@RequiredArgsConstructor
@Tag(name = "系统值合规检查", description = "A8系统值合规检查与审计")
public class SystemValueComplianceController {

    private final ISystemValueComplianceService complianceService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('COMPLIANCE_VIEW')")
    public ApiResponse<List<SystemValueComplianceVO>> listByServer(@RequestParam(required = false) Long serverId) {
        return ApiResponse.success(complianceService.listByServer(serverId));
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAuthority('COMPLIANCE_VIEW')")
    public ApiResponse<SystemValueComplianceVO> getLatestByServer(@RequestParam(required = false) Long serverId) {
        return ApiResponse.success(complianceService.getLatestByServer(serverId));
    }
}
