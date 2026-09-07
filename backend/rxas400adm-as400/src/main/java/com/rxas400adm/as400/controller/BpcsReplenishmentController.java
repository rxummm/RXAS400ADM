package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsReplenishmentService;
import com.rxas400adm.as400.vo.BpcsReplenishmentVO;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;


/**
 * ② 智能补货建议 Controller。
 */
@RestController
@RequestMapping("/api/v1/bpcs/replenishment")
@RequiredArgsConstructor
@Tag(name = "BPCS Replenishment", description = "Replenishment planning and scheduling")
public class BpcsReplenishmentController {

    private final IBpcsReplenishmentService service;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsReplenishmentVO>> list(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(service.getReplenishmentSuggestions(cono, limit));
    }
}
