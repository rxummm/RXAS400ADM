package com.rxas400adm.procurement.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.annotation.OperateLogModule;
import com.rxas400adm.common.annotation.OperateLogOperation;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.procurement.dto.PurchaseApprovalActionDTO;
import com.rxas400adm.procurement.dto.PurchaseOrderCreateDTO;
import com.rxas400adm.procurement.dto.PurchaseOrderQueryDTO;
import com.rxas400adm.procurement.dto.PurchaseOrderUpdateDTO;
import com.rxas400adm.procurement.service.PurchaseOrderService;
import com.rxas400adm.procurement.vo.PurchaseOrderDetailVO;
import com.rxas400adm.procurement.vo.PurchaseOrderVO;
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
 * 采购订单管理（PO Management）。
 * 支持完整 CRUD、多级审批流、状态跟踪。
 * 权限：PO_MANAGE（管理）、PO_APPROVE（审批）。
 */
@RestController
@RequestMapping("/api/v1/procurement/po")
@RequiredArgsConstructor
@Tag(name = "采购订单管理", description = "本地管理 + 多级审批流 + 状态跟踪")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping("/page")
    @PreAuthorize("hasAuthority('PO_MANAGE')")
    @Operation(summary = "分页查询采购订单列表")
    public ApiResponse<PageResult<PurchaseOrderVO>> page(@Valid @RequestBody PurchaseOrderQueryDTO query) {
        return ApiResponse.success(purchaseOrderService.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PO_MANAGE')")
    @Operation(summary = "获取采购订单详情（含行项和审批历史）")
    public ApiResponse<PurchaseOrderDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(purchaseOrderService.getDetail(id));
    }

    @GetMapping("/generate-po-no")
    @PreAuthorize("hasAuthority('PO_MANAGE')")
    @Operation(summary = "生成下一个采购单号")
    public ApiResponse<String> generatePoNo() {
        return ApiResponse.success(purchaseOrderService.generatePoNo());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PO_MANAGE')")
    @OperateLog(module = OperateLogModule.PURCHASE_ORDER, operation = OperateLogOperation.CREATE_PURCHASE_ORDER)
    @Operation(summary = "新建采购订单（DRAFT）")
    public ApiResponse<PurchaseOrderVO> create(@Valid @RequestBody PurchaseOrderCreateDTO dto) {
        return ApiResponse.success(purchaseOrderService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PO_MANAGE')")
    @OperateLog(module = OperateLogModule.PURCHASE_ORDER, operation = OperateLogOperation.UPDATE_PURCHASE_ORDER)
    @Operation(summary = "更新采购订单（仅 DRAFT 状态可编辑）")
    public ApiResponse<PurchaseOrderVO> update(@PathVariable Long id,
                                               @Valid @RequestBody PurchaseOrderUpdateDTO dto) {
        return ApiResponse.success(purchaseOrderService.update(id, dto));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('PO_MANAGE')")
    @OperateLog(module = OperateLogModule.PURCHASE_ORDER, operation = OperateLogOperation.SUBMIT_PURCHASE_APPROVAL)
    @Operation(summary = "提交审批（DRAFT → PENDING_APPROVAL）")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        purchaseOrderService.submitForApproval(id);
        return ApiResponse.success();
    }

    @PostMapping("/approval")
    @PreAuthorize("hasAuthority('PO_APPROVE')")
    @OperateLog(module = OperateLogModule.PURCHASE_ORDER, operation = OperateLogOperation.APPROVE_PURCHASE_ORDER)
    @Operation(summary = "审批操作（APPROVED/REJECTED/RETURNED）")
    public ApiResponse<Void> approval(@Valid @RequestBody PurchaseApprovalActionDTO dto) {
        purchaseOrderService.handleApproval(dto);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('PO_MANAGE')")
    @OperateLog(module = OperateLogModule.PURCHASE_ORDER, operation = OperateLogOperation.RECEIVE_PURCHASE_ORDER)
    @Operation(summary = "收货（APPROVED/SHIPPED → RECEIVED）")
    public ApiResponse<Void> receive(@PathVariable Long id) {
        purchaseOrderService.receiveOrder(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PO_MANAGE')")
    @OperateLog(module = OperateLogModule.PURCHASE_ORDER, operation = OperateLogOperation.CANCEL_PURCHASE_ORDER)
    @Operation(summary = "取消订单（仅 DRAFT/PENDING_APPROVAL 可取消）")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        purchaseOrderService.cancelOrder(id);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PO_MANAGE')")
    @OperateLog(module = OperateLogModule.PURCHASE_ORDER, operation = OperateLogOperation.DELETE_PURCHASE_ORDER)
    @Operation(summary = "删除订单（仅 DRAFT 可删除）")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        purchaseOrderService.deleteOrder(id);
        return ApiResponse.success();
    }
}