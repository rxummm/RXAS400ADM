package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsCustomerQueryDTO;
import com.rxas400adm.as400.service.IBpcsCustomerService;
import com.rxas400adm.as400.vo.BpcsCustomerVO;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * 【AS400 业务增强·P2】BPCS 客户档案查询（只读）。
 * 权限：BPCS_CUSTOMER_VIEW；数据源 RCM + EST 只读查询，零写操作。
 */
@RestController
@RequestMapping("/api/v1/bpcs/customers")
@RequiredArgsConstructor
@Tag(name = "BPCS Customer Master")
public class BpcsCustomerController {

    private final IBpcsCustomerService customerService;

    /** 客户列表搜索 */
    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_CUSTOMER_VIEW')")
    public ApiResponse<PageResult<BpcsCustomerVO>> search(@Valid BpcsCustomerQueryDTO query) {
        return ApiResponse.success(customerService.search(query));
    }

    /** 客户详情（含 Ship-To 列表） */
    @GetMapping("/{cono}/{cust}")
    @PreAuthorize("hasAuthority('BPCS_CUSTOMER_VIEW')")
    public ApiResponse<BpcsCustomerVO> detail(@PathVariable String cono, @PathVariable String cust) {
        return ApiResponse.success(customerService.getDetail(cono, cust));
    }
}
