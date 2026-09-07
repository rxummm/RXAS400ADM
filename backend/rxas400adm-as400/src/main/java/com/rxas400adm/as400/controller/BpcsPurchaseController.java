package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsPurchaseQueryDTO;
import com.rxas400adm.as400.service.IBpcsPurchaseService;
import com.rxas400adm.as400.vo.BpcsPurchaseOrderVO;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 【AS400 业务增强·P2】BPCS 采购订单（只读）。
 * 权限：BPCS_PURCHASE_VIEW；数据源 HPH + HPO，零写操作。
 */
@RestController
@RequestMapping("/api/v1/bpcs/purchases")
@RequiredArgsConstructor
@Tag(name = "BPCS Purchase Orders")
public class BpcsPurchaseController {

    private final IBpcsPurchaseService purchaseService;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_PURCHASE_VIEW')")
    public ApiResponse<PageResult<BpcsPurchaseOrderVO>> search(@Valid BpcsPurchaseQueryDTO query) {
        return ApiResponse.success(purchaseService.search(query));
    }
}
