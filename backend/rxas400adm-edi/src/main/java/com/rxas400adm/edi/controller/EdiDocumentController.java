package com.rxas400adm.edi.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.edi.service.EdiDocumentService;
import com.rxas400adm.edi.vo.EdiDocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/edi/documents")
@RequiredArgsConstructor
@Tag(name = "EDI单据", description = "EDI单据管理")
public class EdiDocumentController {

    private final EdiDocumentService service;

    @GetMapping
    @PreAuthorize("hasAuthority('EDI_DOCUMENT_VIEW')")
    @Operation(summary = "分页查询EDI单据")
    public ApiResponse<PageResult<EdiDocumentVO>> page(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.pageQuery(cono, documentType, direction, status, fromDate, toDate, current, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EDI_DOCUMENT_VIEW')")
    @Operation(summary = "获取EDI单据详情")
    public ApiResponse<EdiDocumentVO> get(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
