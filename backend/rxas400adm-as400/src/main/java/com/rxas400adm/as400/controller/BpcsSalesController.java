package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsSalesQueryDTO;
import com.rxas400adm.as400.service.IBpcsSalesService;
import com.rxas400adm.as400.vo.BpcsSalesTrendVO;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【AS400 业务增强·P2】BPCS 销售趋势（只读）。
 * 权限：BPCS_SALES_VIEW；数据源 SSH/SSD 按月聚合，零写操作。
 */
@RestController
@RequestMapping("/api/v1/bpcs/sales")
@RequiredArgsConstructor
@Tag(name = "BPCS销售趋势")
public class BpcsSalesController {

    private final IBpcsSalesService salesService;

    @GetMapping("/trend")
    @PreAuthorize("hasAuthority('BPCS_SALES_VIEW')")
    public ApiResponse<BpcsSalesTrendVO> trend(@Valid BpcsSalesQueryDTO query) {
        return ApiResponse.success(salesService.getTrend(query));
    }
}
