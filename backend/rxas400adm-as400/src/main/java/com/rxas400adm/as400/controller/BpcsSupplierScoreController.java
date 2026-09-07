package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsSupplierScoreService;
import com.rxas400adm.as400.vo.BpcsSupplierScoreVO;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;


/**
 * ④ 供应商评分 Controller。
 */
@RestController
@RequestMapping("/api/v1/bpcs/supplierScore")
@RequiredArgsConstructor
@Tag(name = "BPCS Supplier Score", description = "Supplier score and evaluation")
public class BpcsSupplierScoreController {

    private final IBpcsSupplierScoreService service;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsSupplierScoreVO>> list(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(service.getSupplierScores(cono, limit));
    }
}
