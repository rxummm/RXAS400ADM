package com.rxas400adm.quality.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.annotation.OperateLogModule;
import com.rxas400adm.common.annotation.OperateLogOperation;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.quality.dto.NcrCreateDTO;
import com.rxas400adm.quality.dto.NcrUpdateDTO;
import com.rxas400adm.quality.entity.Ncr;
import com.rxas400adm.quality.service.NcrService;
import com.rxas400adm.quality.vo.NcrVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/quality/ncr")
@RequiredArgsConstructor
@Tag(name = "不合格品管理", description = "NCR不合格品报告")
public class NcrController {

    private final NcrService service;

    @GetMapping
    @PreAuthorize("hasAuthority('QUALITY_NCR_VIEW')")
    @Operation(summary = "分页查询NCR列表")
    public ApiResponse<PageResult<NcrVO>> page(
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.pageQuery(itemCode, status, fromDate, toDate, current, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('QUALITY_NCR_VIEW')")
    @Operation(summary = "获取NCR详情")
    public ApiResponse<NcrVO> get(@PathVariable Long id) {
        return ApiResponse.success(NcrVO.from(service.getByIdOrThrow(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('QUALITY_NCR_CREATE')")
    @OperateLog(module = OperateLogModule.QUALITY, operation = OperateLogOperation.CREATE)
    @Operation(summary = "创建NCR报告")
    public ApiResponse<Long> create(@Valid @RequestBody NcrCreateDTO dto) {
        Ncr ncr = service.create(dto);
        return ApiResponse.success(ncr.getId());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('QUALITY_NCR_UPDATE')")
    @OperateLog(module = OperateLogModule.QUALITY, operation = OperateLogOperation.UPDATE)
    @Operation(summary = "更新NCR状态")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody NcrUpdateDTO dto) {
        service.updateStatus(id, dto);
        return ApiResponse.success(null);
    }
}