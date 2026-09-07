package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsItemQueryDTO;
import com.rxas400adm.as400.service.IBpcsItemService;
import com.rxas400adm.as400.vo.BpcsItemVO;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * 【AS400 业务增强·P2】BPCS 物料主档（只读，多维度 Tab）。
 * 权限：BPCS_ITEM_VIEW；数据源 IIM + IWI + HPO + SSD，零写操作。
 */
@RestController
@RequestMapping("/api/v1/bpcs/items")
@RequiredArgsConstructor
@Tag(name = "BPCS Item Master")
public class BpcsItemController {

    private final IBpcsItemService itemService;

    /** 物料列表搜索 */
    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_ITEM_VIEW')")
    public ApiResponse<PageResult<BpcsItemVO>> search(@Valid BpcsItemQueryDTO query) {
        return ApiResponse.success(itemService.search(query));
    }

    /** 物料详情（含库存/采购/销售全维度 Tab 数据） */
    @GetMapping("/{item}")
    @PreAuthorize("hasAuthority('BPCS_ITEM_VIEW')")
    public ApiResponse<BpcsItemVO> detail(@PathVariable String item) {
        return ApiResponse.success(itemService.getDetail(item));
    }
}
