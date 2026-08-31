package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsAbcXyzService;
import com.rxas400adm.as400.vo.BpcsInventoryAbcXyzVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ABC/XYZ 矩阵分析 Controller（㊲）。
 * 权限：BPCS_INVENTORY_VIEW；只读查询。
 */
@RestController
@RequestMapping("/api/v1/bpcs/inventory/abc-xyz")
@RequiredArgsConstructor
@Tag(name = "BPCS ABC/XYZ矩阵分析")
public class BpcsAbcXyzController {

    private final IBpcsAbcXyzService abcXyzService;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    @Operation(summary = "ABC/XYZ 矩阵分析")
    public ApiResponse<List<BpcsInventoryAbcXyzVO>> matrix(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "20250101") String fromDate,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(abcXyzService.getMatrix(cono, fromDate, limit));
    }
}
