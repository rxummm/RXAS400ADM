package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsKanbanService;
import com.rxas400adm.as400.vo.BpcsKanbanVO;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ⑯ 订单看板视图 Controller。
 */
@RestController
@RequestMapping("/api/v1/bpcs/kanban")
@RequiredArgsConstructor
public class BpcsKanbanController {

    private final IBpcsKanbanService service;

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsKanbanVO>> orders(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(service.listKanbanOrders(cono, limit));
    }
}
