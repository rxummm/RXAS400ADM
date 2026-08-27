package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsShippingQueryDTO;
import com.rxas400adm.as400.service.IBpcsShippingService;
import com.rxas400adm.as400.vo.BpcsLoadVO;
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
 * 【AS400 业务增强·P2】BPCS 发运/载荷看板（只读）。
 * 权限：BPCS_SHIPPING_VIEW；数据源 LLH 只读查询，零写操作。
 */
@RestController
@RequestMapping("/api/v1/bpcs/shipping")
@RequiredArgsConstructor
@Tag(name = "BPCS发运看板")
public class BpcsShippingController {

    private final IBpcsShippingService shippingService;

    /** 载荷列表（可按状态/载荷号筛选） */
    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_SHIPPING_VIEW')")
    public ApiResponse<PageResult<BpcsLoadVO>> search(@Valid BpcsShippingQueryDTO query) {
        return ApiResponse.success(shippingService.search(query));
    }
}
