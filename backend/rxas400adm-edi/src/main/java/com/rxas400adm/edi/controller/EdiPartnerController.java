package com.rxas400adm.edi.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.edi.service.EdiPartnerService;
import com.rxas400adm.edi.vo.EdiPartnerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/edi/partners")
@RequiredArgsConstructor
@Tag(name = "EDI合作伙伴", description = "EDI合作伙伴管理")
public class EdiPartnerController {

    private final EdiPartnerService service;

    @GetMapping
    @PreAuthorize("hasAuthority('EDI_PARTNER_VIEW')")
    @Operation(summary = "分页查询合作伙伴")
    public ApiResponse<PageResult<EdiPartnerVO>> page(
            @RequestParam(required = false) String partnerType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.pageQuery(partnerType, status, current, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EDI_PARTNER_VIEW')")
    @Operation(summary = "获取合作伙伴详情")
    public ApiResponse<EdiPartnerVO> get(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
