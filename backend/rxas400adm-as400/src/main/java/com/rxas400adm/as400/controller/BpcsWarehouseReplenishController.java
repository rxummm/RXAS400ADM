package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsWarehouseReplenishQueryDTO;
import com.rxas400adm.as400.service.IBpcsWarehouseReplenishService;
import com.rxas400adm.as400.vo.BpcsWarehouseReplenishVO;
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
 * 多仓库联合补货查询（只读）。
 * 权限：BPCS_VIEW；数据源 IWI + IIM + ITL + HPO，零写操作。
 */
@RestController
@RequestMapping("/api/v1/bpcs/warehouse-replenish")
@RequiredArgsConstructor
@Tag(name = "BPCS多仓库联合补货")
public class BpcsWarehouseReplenishController {

    private final IBpcsWarehouseReplenishService replenishService;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<PageResult<BpcsWarehouseReplenishVO>> search(@Valid BpcsWarehouseReplenishQueryDTO query) {
        return ApiResponse.success(replenishService.search(query));
    }
}
