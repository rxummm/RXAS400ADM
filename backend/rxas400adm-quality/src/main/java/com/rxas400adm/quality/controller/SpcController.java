package com.rxas400adm.quality.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.quality.entity.SpcRecord;
import com.rxas400adm.quality.service.SpcService;
import com.rxas400adm.quality.vo.SpcRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quality/spc")
@RequiredArgsConstructor
@Tag(name = "SPC统计过程控制", description = "过程控制数据分析")
public class SpcController {

    private final SpcService service;

    @GetMapping
    @PreAuthorize("hasAuthority('QUALITY_SPC_VIEW')")
    @Operation(summary = "分页查询SPC记录")
    public ApiResponse<PageResult<SpcRecordVO>> page(
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        PageResult<SpcRecord> pageResult = service.pageQuery(itemCode, fromDate, toDate, current, size);
        List<SpcRecordVO> voList = pageResult.getRecords().stream()
                .map(SpcRecordVO::from)
                .toList();
        return ApiResponse.success(new PageResult<>(pageResult.getTotal(), voList));
    }

    @GetMapping("/out-of-control")
    @PreAuthorize("hasAuthority('QUALITY_SPC_VIEW')")
    @Operation(summary = "查询失控记录")
    public ApiResponse<PageResult<SpcRecordVO>> listOutOfControl(
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        PageResult<SpcRecord> pageResult = service.pageOutOfControl(itemCode, fromDate, current, size);
        List<SpcRecordVO> voList = pageResult.getRecords().stream()
                .map(SpcRecordVO::from)
                .toList();
        return ApiResponse.success(new PageResult<>(pageResult.getTotal(), voList));
    }
}
