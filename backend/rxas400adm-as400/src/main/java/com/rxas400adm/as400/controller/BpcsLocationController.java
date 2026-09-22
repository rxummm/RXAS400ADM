package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsLocationService;
import com.rxas400adm.as400.vo.BpcsLocationInventoryVO;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/bpcs/location")
@RequiredArgsConstructor
@Tag(name = "BPCS Location", description = "Location and warehouse mapping")
public class BpcsLocationController {
    private final IBpcsLocationService service;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<PageResult<BpcsLocationInventoryVO>> list(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.listLocationInventory(cono, current, size));
    }
}
