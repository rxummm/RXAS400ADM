package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsCreditHoldService;
import com.rxas400adm.as400.vo.BpcsCreditHoldVO;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;


/**
 * ⑪ 信用 Hold 管理 Controller。
 */
@RestController
@RequestMapping("/api/v1/bpcs/creditHold")
@RequiredArgsConstructor
@Tag(name = "BPCS Credit Hold", description = "Credit hold management")
public class BpcsCreditHoldController {

    private final IBpcsCreditHoldService service;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsCreditHoldVO>> list(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(service.listHoldOrders(cono, limit));
    }
}
