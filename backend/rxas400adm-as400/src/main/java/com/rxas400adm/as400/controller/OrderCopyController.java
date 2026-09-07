package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IOrderCopyService;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 订单复制 Controller。
 */
@RestController
@RequestMapping("/api/v1/bpcs/orderCopy")
@RequiredArgsConstructor
@Tag(name = "BPCS Order Copy", description = "Order copy operations")
public class OrderCopyController {

    private final IOrderCopyService service;

    @PostMapping
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "BPCS", operation = "复制订单")
    public ApiResponse<String> copy(
            @RequestParam String cono,
            @RequestParam String sourceOrno) {
        return service.copyOrder(cono, sourceOrno, SecurityUtils.currentUsername());
    }
}
