package com.rxas400adm.quality.controller;

import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.quality.service.TraceabilityService;
import com.rxas400adm.quality.vo.TraceabilityChainVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quality/traceability")
@RequiredArgsConstructor
@Tag(name = "质量追溯", description = "批次追溯链查询")
public class TraceabilityController {

    private final TraceabilityService service;

    @GetMapping("/upstream")
    @PreAuthorize("hasAuthority('QUALITY_TRACE_VIEW')")
    @Operation(summary = "上游追溯")
    public ApiResponse<List<TraceabilityChainVO>> traceUpstream(
            @RequestParam String itemCode,
            @RequestParam String batchNo) {
        List<TraceabilityChainVO> voList = service.traceUpstream(itemCode, batchNo).stream()
                .map(TraceabilityChainVO::from)
                .toList();
        return ApiResponse.success(voList);
    }

    @GetMapping("/downstream")
    @PreAuthorize("hasAuthority('QUALITY_TRACE_VIEW')")
    @Operation(summary = "下游追溯")
    public ApiResponse<List<TraceabilityChainVO>> traceDownstream(
            @RequestParam String itemCode,
            @RequestParam String batchNo) {
        List<TraceabilityChainVO> voList = service.traceDownstream(itemCode, batchNo).stream()
                .map(TraceabilityChainVO::from)
                .toList();
        return ApiResponse.success(voList);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('QUALITY_TRACE_VIEW')")
    @Operation(summary = "分页追溯查询")
    public ApiResponse<PageResult<TraceabilityChainVO>> tracePaged(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String traceType) {
        PageResult<TraceabilityChainVO> result = service.tracePaged(current, size, itemCode, batchNo, traceType)
                .map(TraceabilityChainVO::from);
        return ApiResponse.success(result);
    }
}
