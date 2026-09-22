package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.RmaDTO;
import com.rxas400adm.as400.dto.RmaStatusUpdateDTO;
import com.rxas400adm.as400.entity.Rma;
import com.rxas400adm.as400.service.IRmaService;
import com.rxas400adm.as400.vo.RmaVO;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * 退货 RMA Controller（CRUD）。
 */
@RestController
@RequestMapping("/api/v1/bpcs/rma")
@RequiredArgsConstructor
@Tag(name = "BPCS RMA", description = "Return merchandise authorization")
public class RmaController {

    private final IRmaService service;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<PageResult<RmaVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Rma> page = service.list(status, current, size);
        return ApiResponse.success(new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(RmaVO::from).toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<RmaVO> get(@PathVariable Long id) {
        return ApiResponse.success(RmaVO.from(service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "Create RMA")
    public ApiResponse<RmaVO> create(@Valid @RequestBody RmaDTO dto) {
        return ApiResponse.success(RmaVO.from(service.create(dto, SecurityUtils.currentUsername())));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "Update RMA status")
    public ApiResponse<RmaVO> updateStatus(@PathVariable Long id, @Valid @RequestBody RmaStatusUpdateDTO dto) {
        return ApiResponse.success(RmaVO.from(service.updateStatus(id, dto.getStatus(), SecurityUtils.currentUsername())));
    }
}