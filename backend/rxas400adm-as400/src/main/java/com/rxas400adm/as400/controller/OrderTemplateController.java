package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.OrderTemplateDTO;
import com.rxas400adm.as400.service.IOrderTemplateService;
import com.rxas400adm.as400.vo.OrderTemplateVO;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.annotation.OperateLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;


/**
 * 订单模板 Controller（CRUD）。
 */
@RestController
@RequestMapping("/api/v1/bpcs/orderTemplate")
@RequiredArgsConstructor
@Tag(name = "BPCS Order Template", description = "Order template management")
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
    @OperateLog(module = "BPCS", operation = "Create order template")
    public ApiResponse<OrderTemplateVO> create(@Valid @RequestBody OrderTemplateDTO dto) {
        return ApiResponse.success(OrderTemplateVO.from(service.create(dto)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "Update order template")
    public ApiResponse<OrderTemplateVO> update(@Valid @RequestBody OrderTemplateDTO dto) {
        return ApiResponse.success(OrderTemplateVO.from(service.update(dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "Delete order template")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/use")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @OperateLog(module = "BPCS", operation = "Use order template")
    public ApiResponse<OrderTemplateVO> use(@PathVariable Long id) {
        return ApiResponse.success(OrderTemplateVO.from(service.useTemplate(id)));
    }
}
