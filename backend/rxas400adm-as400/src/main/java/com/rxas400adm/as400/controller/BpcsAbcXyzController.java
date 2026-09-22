package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsAbcXyzService;
import com.rxas400adm.as400.vo.BpcsInventoryAbcXyzVO;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bpcs/inventory/abc-xyz")
@RequiredArgsConstructor
@Tag(name = "BPCS ABC/XYZ Analysis")
public class BpcsAbcXyzController {

    private final IBpcsAbcXyzService abcXyzService;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    @Operation(summary = "ABC/XYZ 矩阵分析")
    public ApiResponse<PageResult<BpcsInventoryAbcXyzVO>> matrix(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "20250101") String fromDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(abcXyzService.getMatrix(cono, fromDate, current, size));
    }
}
