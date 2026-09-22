package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsReplenishmentService;
import com.rxas400adm.as400.vo.BpcsReplenishmentVO;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/bpcs/replenishment")
@RequiredArgsConstructor
@Tag(name = "BPCS Replenishment", description = "Replenishment planning and scheduling")
public class BpcsReplenishmentController {
    private final IBpcsReplenishmentService service;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<PageResult<BpcsReplenishmentVO>> list(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.getReplenishmentSuggestions(cono, current, size));
    }
}
