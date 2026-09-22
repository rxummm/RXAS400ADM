package com.rxas400adm.tpm.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.tpm.entity.MaintenancePlan;
import com.rxas400adm.tpm.service.MaintenanceService;
import com.rxas400adm.tpm.vo.MaintenancePlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/tpm/maintenance")
@RequiredArgsConstructor
@Tag(name = "维护计划管理", description = "TPM维护计划查询")
public class MaintenanceController {

    private final MaintenanceService service;

    @GetMapping
    @PreAuthorize("hasAuthority('MAINTENANCE_PLAN_VIEW')")
    @Operation(summary = "分页查询维护计划列表")
    public ApiResponse<PageResult<MaintenancePlanVO>> page(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        PageResult<MaintenancePlan> page = service.pageQuery(cono, status, fromDate, toDate, current, size);
        return ApiResponse.success(page.map(MaintenancePlanVO::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MAINTENANCE_PLAN_VIEW')")
    @Operation(summary = "获取维护计划详情")
    public ApiResponse<MaintenancePlanVO> get(@PathVariable Long id) {
        return ApiResponse.success(MaintenancePlanVO.from(service.getById(id)));
    }
}
