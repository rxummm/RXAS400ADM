package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.BpcsInvoiceQueryDTO;
import com.rxas400adm.as400.service.IBpcsInvoiceService;
import com.rxas400adm.as400.vo.BpcsInvoiceVO;
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
 * 【AS400 业务增强·P2】BPCS 发票轨迹（只读）。
 * 权限：BPCS_INVOICE_VIEW；数据源 BBH/BBL（在制）+ SIH/SIL（历史），零写操作。
 */
@RestController
@RequestMapping("/api/v1/bpcs/invoices")
@RequiredArgsConstructor
@Tag(name = "BPCS发票轨迹")
public class BpcsInvoiceController {

    private final IBpcsInvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_INVOICE_VIEW')")
    public ApiResponse<PageResult<BpcsInvoiceVO>> search(@Valid BpcsInvoiceQueryDTO query) {
        return ApiResponse.success(invoiceService.search(query));
    }
}
