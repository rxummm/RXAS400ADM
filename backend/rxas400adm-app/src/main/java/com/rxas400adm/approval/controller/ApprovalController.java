package com.rxas400adm.approval.controller;

import com.rxas400adm.approval.dto.ApprovalActionDTO;
import com.rxas400adm.approval.service.ApprovalNotificationService;
import com.rxas400adm.approval.vo.ApprovalNotificationVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.annotation.OperateLogModule;
import com.rxas400adm.common.annotation.OperateLogOperation;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Tag(name = "Mobile Approval")
public class ApprovalController {

    private final ApprovalNotificationService approvalService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('APPROVAL_VIEW', 'PO_APPROVE', 'INVOICE_APPROVE')")
    @Operation(summary = "待审批列表")
    public ApiResponse<PageResult<ApprovalNotificationVO>> listPending(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(approvalService.listPending(current, size));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('APPROVAL_VIEW')")
    @Operation(summary = "全部审批历史")
    public ApiResponse<PageResult<ApprovalNotificationVO>> listAll(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(approvalService.listAll(current, size, status, targetType));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('APPROVAL_EXECUTE', 'PO_APPROVE', 'INVOICE_APPROVE')")
    @Operation(summary = "执行审批")
    @OperateLog(module = OperateLogModule.APPROVAL, operation = OperateLogOperation.APPROVE)
    public ApiResponse<ApprovalNotificationVO> approve(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalActionDTO dto) {
        return ApiResponse.success(approvalService.approve(id, dto));
    }

    @GetMapping("/pending/count")
    @PreAuthorize("hasAnyAuthority('APPROVAL_VIEW')")
    @Operation(summary = "未读审批数量")
    public ApiResponse<Long> countPending() {
        return ApiResponse.success(approvalService.countPending());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('APPROVAL_VIEW')")
    @Operation(summary = "审批统计卡片数据（已批准/已驳回数量）")
    public ApiResponse<long[]> getStats() {
        return ApiResponse.success(approvalService.countApprovalStats());
    }
}