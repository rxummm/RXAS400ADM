package com.rxas400adm.quality.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.quality.entity.QualityInspection;
import com.rxas400adm.quality.service.QualityInspectionService;
import com.rxas400adm.quality.vo.QualityInspectionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quality/inspections")
@RequiredArgsConstructor
@Tag(name = "质量检验", description = "质量检验记录管理")
public class QualityInspectionController {

    private final QualityInspectionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('QUALITY_INSPECTION_VIEW')")
    @Operation(summary = "分页查询检验记录")
    public ApiResponse<PageResult<QualityInspectionVO>> page(
            @RequestParam(required = false) String inspectionType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        PageResult<QualityInspection> pageResult = service.pageQuery(inspectionType, result, itemCode, batchNo, fromDate, toDate, current, size);
        List<QualityInspectionVO> voList = pageResult.getRecords().stream()
                .map(QualityInspectionVO::from)
                .toList();
        return ApiResponse.success(new PageResult<>(pageResult.getTotal(), voList));
    }

    @GetMapping("/by-item/{itemCode}")
    @PreAuthorize("hasAuthority('QUALITY_INSPECTION_VIEW')")
    @Operation(summary = "按物料查询检验记录")
    public ApiResponse<PageResult<QualityInspectionVO>> listByItem(
            @PathVariable String itemCode,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        PageResult<QualityInspection> pageResult = service.pageByItemCode(itemCode, current, size);
        List<QualityInspectionVO> voList = pageResult.getRecords().stream()
                .map(QualityInspectionVO::from)
                .toList();
        return ApiResponse.success(new PageResult<>(pageResult.getTotal(), voList));
    }

    @GetMapping("/by-batch/{batchNo}")
    @PreAuthorize("hasAuthority('QUALITY_INSPECTION_VIEW')")
    @Operation(summary = "按批次查询检验记录")
    public ApiResponse<PageResult<QualityInspectionVO>> listByBatch(
            @PathVariable String batchNo,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        PageResult<QualityInspection> pageResult = service.pageByBatchNo(batchNo, current, size);
        List<QualityInspectionVO> voList = pageResult.getRecords().stream()
                .map(QualityInspectionVO::from)
                .toList();
        return ApiResponse.success(new PageResult<>(pageResult.getTotal(), voList));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('QUALITY_INSPECTION_VIEW')")
    @Operation(summary = "获取检验记录详情")
    public ApiResponse<QualityInspectionVO> get(@PathVariable Long id) {
        return ApiResponse.success(QualityInspectionVO.from(service.getByIdOrThrow(id)));
    }
}