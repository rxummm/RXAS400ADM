package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IOrderChangeService;
import com.rxas400adm.as400.vo.OrderChangeVO;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单变更管理 Controller（只读查询）。
 */
@RestController
@RequestMapping("/api/v1/bpcs/orderChange")
@RequiredArgsConstructor
public class OrderChangeController {

    private final IOrderChangeService service;

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<OrderChangeVO>> list(
            @RequestParam String cono,
            @RequestParam String orno) {
        return ApiResponse.success(service.listByOrder(cono, orno).stream().map(OrderChangeVO::from).toList());
    }
}
