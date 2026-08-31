package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.OrderTemplateDTO;
import com.rxas400adm.as400.service.IOrderTemplateService;
import com.rxas400adm.as400.vo.OrderTemplateVO;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.annotation.OperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单模板 Controller（CRUD）。
 */
@RestController
@RequestMapping("/api/v1/bpcs/orderTemplate")
@RequiredArgsConstructor
public class OrderTemplateController {

    private final IOrderTemplateService service;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<OrderTemplateVO>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(service.list(keyword).stream().map(OrderTemplateVO::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<OrderTemplateVO> get(@PathVariable Long id) {
        return ApiResponse.success(OrderTemplateVO.from(service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "创建订单模板")
    public ApiResponse<OrderTemplateVO> create(@RequestBody OrderTemplateDTO dto) {
        return ApiResponse.success(OrderTemplateVO.from(service.create(dto)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "更新订单模板")
    public ApiResponse<OrderTemplateVO> update(@RequestBody OrderTemplateDTO dto) {
        return ApiResponse.success(OrderTemplateVO.from(service.update(dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "删除订单模板")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/use")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @OperateLog(module = "BPCS", operation = "使用订单模板")
    public ApiResponse<OrderTemplateVO> use(@PathVariable Long id) {
        return ApiResponse.success(OrderTemplateVO.from(service.useTemplate(id)));
    }
}
