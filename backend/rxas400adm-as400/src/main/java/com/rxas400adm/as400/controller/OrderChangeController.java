package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.entity.OrderChange;
import com.rxas400adm.as400.service.IOrderChangeService;
import com.rxas400adm.as400.vo.OrderChangeVO;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * 订单变更管理 Controller（只读查询）。
 */
@RestController
@RequestMapping("/api/v1/bpcs/orderChange")
@RequiredArgsConstructor
@Tag(name = "BPCS Order Change", description = "Order change management")
public class OrderChangeController {

    private final IOrderChangeService service;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<PageResult<OrderChangeVO>> list(
            @RequestParam String cono,
            @RequestParam String orno,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<OrderChange> page = service.listByOrder(cono, orno, current, size);
        return ApiResponse.success(new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(OrderChangeVO::from).toList()));
    }
}