package com.rxas400adm.mrp.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.annotation.OperateLogModule;
import com.rxas400adm.common.annotation.OperateLogOperation;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.mrp.service.MrpDemandService;
import com.rxas400adm.mrp.vo.MrpDemandVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mrp/demands")
@RequiredArgsConstructor
@Tag(name = "MRP需求管理", description = "物料需求计划管理")
public class MrpDemandController {

    private final MrpDemandService service;

    @GetMapping
    @PreAuthorize("hasAuthority('MRP_RUN')")
    @Operation(summary = "分页查询需求列表")
    public ApiResponse<PageResult<MrpDemandVO>> page(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String demandType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.pageQuery(cono, itemCode, demandType, status, current, size));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MRP_RUN')")
    @OperateLog(module = OperateLogModule.MRP, operation = OperateLogOperation.UPDATE)
    @Operation(summary = "更新需求状态")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        service.updateStatus(id, status);
        return ApiResponse.success();
    }
}