package com.rxas400adm.finance.ar.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.annotation.OperateLogModule;
import com.rxas400adm.common.annotation.OperateLogOperation;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.finance.ar.dto.ArInvoiceCreateDTO;
import com.rxas400adm.finance.ar.dto.ArInvoiceQueryDTO;
import com.rxas400adm.finance.ar.dto.ArInvoiceUpdateDTO;
import com.rxas400adm.finance.ar.dto.ArPaymentCreateDTO;
import com.rxas400adm.finance.ar.service.IArInvoiceService;
import com.rxas400adm.finance.ar.vo.ArInvoiceDetailVO;
import com.rxas400adm.finance.ar.vo.ArInvoiceVO;
import com.rxas400adm.finance.ar.vo.ArPaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 应收账款管理（AR Management）。
 * 支持完整 CRUD、收款核销、账龄分析。
 * 权限：AR_MANAGE（管理）。
 */
@RestController
@RequestMapping("/api/v1/ar/invoice")
@RequiredArgsConstructor
@Tag(name = "应收账款管理", description = "客户账单 + 收款核销 + 账龄分析")
public class ArInvoiceController {

    private final IArInvoiceService invoiceService;

    @PostMapping("/page")
    @PreAuthorize("hasAuthority('AR_MANAGE')")
    @Operation(summary = "分页查询应收账款发票列表")
    public ApiResponse<PageResult<ArInvoiceVO>> page(@Valid @RequestBody ArInvoiceQueryDTO query) {
        return ApiResponse.success(invoiceService.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AR_MANAGE')")
    @Operation(summary = "获取应收账款发票详情（含收款记录）")
    public ApiResponse<ArInvoiceDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(invoiceService.getDetail(id));
    }

    @GetMapping("/generate-invoice-no")
    @PreAuthorize("hasAuthority('AR_MANAGE')")
    @Operation(summary = "生成下一个账单号")
    public ApiResponse<String> generateInvoiceNo() {
        return ApiResponse.success(invoiceService.generateInvoiceNo());
    }

    @GetMapping("/generate-payment-no")
    @PreAuthorize("hasAuthority('AR_MANAGE')")
    @Operation(summary = "生成下一个收款单号")
    public ApiResponse<String> generatePaymentNo() {
        return ApiResponse.success(invoiceService.generatePaymentNo());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('AR_MANAGE')")
    @OperateLog(module = OperateLogModule.FINANCE_AR, operation = OperateLogOperation.CREATE_AR_INVOICE)
    @Operation(summary = "新建应收账款发票（DRAFT）")
    public ApiResponse<ArInvoiceVO> create(@Valid @RequestBody ArInvoiceCreateDTO dto) {
        return ApiResponse.success(invoiceService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('AR_MANAGE')")
    @OperateLog(module = OperateLogModule.FINANCE_AR, operation = OperateLogOperation.UPDATE_AR_INVOICE)
    @Operation(summary = "更新应收账款发票（仅 DRAFT 状态可编辑）")
    public ApiResponse<ArInvoiceVO> update(@PathVariable Long id,
                                           @Valid @RequestBody ArInvoiceUpdateDTO dto) {
        return ApiResponse.success(invoiceService.update(id, dto));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('AR_MANAGE')")
    @OperateLog(module = OperateLogModule.FINANCE_AR, operation = OperateLogOperation.SUBMIT_AR_INVOICE)
    @Operation(summary = "提交发票（DRAFT → OPEN）")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        invoiceService.submit(id);
        return ApiResponse.success();
    }

    @PostMapping("/receive-payment")
    @PreAuthorize("hasAuthority('AR_MANAGE')")
    @OperateLog(module = OperateLogModule.FINANCE_AR, operation = OperateLogOperation.RECORD_AR_PAYMENT)
    @Operation(summary = "记录收款（核销）")
    public ApiResponse<ArPaymentVO> receivePayment(@Valid @RequestBody ArPaymentCreateDTO dto) {
        return ApiResponse.success(invoiceService.receivePayment(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('AR_MANAGE')")
    @OperateLog(module = OperateLogModule.FINANCE_AR, operation = OperateLogOperation.DELETE_AR_INVOICE)
    @Operation(summary = "删除应收账款发票（仅 DRAFT 可删除）")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ApiResponse.success();
    }
}
