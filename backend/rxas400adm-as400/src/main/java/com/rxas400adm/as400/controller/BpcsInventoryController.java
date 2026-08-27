package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsInventoryQueryDTO;
import com.rxas400adm.as400.service.IBpcsInventoryService;
import com.rxas400adm.as400.vo.BpcsInventoryVO;
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
 * 【AS400 业务增强·P2】BPCS 库存可用量查询（只读）。
 * 权限：BPCS_INVENTORY_VIEW；数据源 IIM + IWI 只读查询，零写操作。
 */
@RestController
@RequestMapping("/api/v1/bpcs/inventory")
@RequiredArgsConstructor
@Tag(name = "BPCS库存可用量")
public class BpcsInventoryController {

    private final IBpcsInventoryService inventoryService;

    /** 库存可用量搜索（按物料号/描述/仓库） */
    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_INVENTORY_VIEW')")
    public ApiResponse<PageResult<BpcsInventoryVO>> search(@Valid BpcsInventoryQueryDTO query) {
        return ApiResponse.success(inventoryService.search(query));
    }
}
