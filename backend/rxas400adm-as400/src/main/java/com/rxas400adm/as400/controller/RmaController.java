package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.RmaDTO;
import com.rxas400adm.as400.service.IRmaService;
import com.rxas400adm.as400.vo.RmaVO;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 退货 RMA Controller（CRUD）。
 */
@RestController
@RequestMapping("/api/v1/bpcs/rma")
@RequiredArgsConstructor
public class RmaController {

    private final IRmaService service;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<RmaVO>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(service.list(status).stream().map(RmaVO::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<RmaVO> get(@PathVariable Long id) {
        return ApiResponse.success(RmaVO.from(service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "创建 RMA")
    public ApiResponse<RmaVO> create(@RequestBody RmaDTO dto) {
        return ApiResponse.success(RmaVO.from(service.create(dto, "admin")));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "更新 RMA 状态")
    public ApiResponse<RmaVO> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.success(RmaVO.from(service.updateStatus(id, body.get("status"), "admin")));
    }
}
