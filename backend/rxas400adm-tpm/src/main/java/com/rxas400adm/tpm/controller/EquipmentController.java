package com.rxas400adm.tpm.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.tpm.service.EquipmentService;
import com.rxas400adm.tpm.vo.EquipmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tpm/equipment")
@RequiredArgsConstructor
@Tag(name = "设备管理", description = "TPM设备信息查询")
public class EquipmentController {

    private final EquipmentService service;

    @GetMapping
    @PreAuthorize("hasAuthority('EQUIPMENT_VIEW')")
    @Operation(summary = "分页查询设备列表")
    public ApiResponse<PageResult<EquipmentVO>> page(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.pageQuery(cono, status, location, department, current, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EQUIPMENT_VIEW')")
    @Operation(summary = "获取设备详情")
    public ApiResponse<EquipmentVO> get(@PathVariable Long id) {
        return ApiResponse.success(EquipmentVO.from(service.getById(id)));
    }
}
