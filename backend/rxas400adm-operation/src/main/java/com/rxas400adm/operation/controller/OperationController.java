package com.rxas400adm.operation.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.annotation.OperateLogModule;
import com.rxas400adm.common.annotation.OperateLogOperation;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.SecurityUtils;
import com.rxas400adm.operation.dto.CreateOperationRequest;
import com.rxas400adm.operation.executor.OperationExecutor;
import com.rxas400adm.operation.service.OperationRegistry;
import com.rxas400adm.operation.service.OperationService;
import com.rxas400adm.operation.vo.OperationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
@Tag(name = "Operation Management")
public class OperationController {

    private final OperationService operationService;
    private final OperationRegistry registry;

    @PostMapping
    @PreAuthorize("hasAuthority('OPERATION_EXECUTE')")
    @OperateLog(module = OperateLogModule.OPERATION, operation = OperateLogOperation.CREATE_AND_EXECUTE)
    @Operation(summary = "Create and execute an Operation")
    public ApiResponse<OperationVO> create(@Valid @RequestBody CreateOperationRequest req) {
        var op = operationService.create(
            new CreateOperationRequest(
                req.operationType(), req.targetType(), req.targetName(),
                req.requestData(), req.idempotencyKey(),
                SecurityUtils.currentUsername()
            )
        );
        op = operationService.execute(op.getId());
        List<String> steps = resolveSteps(req.operationType());
        return ApiResponse.success(OperationVO.from(op, steps));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERATION_VIEW')")
    @Operation(summary = "Get Operation status")
    public ApiResponse<OperationVO> get(@PathVariable Long id) {
        var op = operationService.get(id);
        List<String> steps = resolveSteps(op.getOperationType());
        return ApiResponse.success(OperationVO.from(op, steps));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('OPERATION_VIEW')")
    @Operation(summary = "List Operations with pagination")
    public ApiResponse<PageResult<OperationVO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        var result = operationService.list(current, size);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('OPERATION_EXECUTE')")
    @OperateLog(module = OperateLogModule.OPERATION, operation = OperateLogOperation.RETRY)
    @Operation(summary = "Retry a failed Operation")
    public ApiResponse<OperationVO> retry(@PathVariable Long id) {
        var op = operationService.retry(id);
        List<String> steps = resolveSteps(op.getOperationType());
        return ApiResponse.success(OperationVO.from(op, steps));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('OPERATION_EXECUTE')")
    @OperateLog(module = OperateLogModule.OPERATION, operation = OperateLogOperation.CANCEL)
    @Operation(summary = "Cancel an Operation")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        operationService.cancel(id);
        return ApiResponse.success(null);
    }

    private List<String> resolveSteps(String operationType) {
        OperationExecutor executor = registry.get(operationType);
        return executor != null ? executor.defineSteps() : Collections.emptyList();
    }
}