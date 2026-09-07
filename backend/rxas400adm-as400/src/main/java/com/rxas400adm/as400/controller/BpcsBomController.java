package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.service.IBpcsBomService;
import com.rxas400adm.as400.vo.BpcsBomLineVO;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;


/**
 * ① BOM 查询展开 Controller。
 */
@RestController
@RequestMapping("/api/v1/bpcs/bom")
@RequiredArgsConstructor
@Tag(name = "BPCS BOM", description = "Bill of materials management")
public class BpcsBomController {

    private final IBpcsBomService service;

    @GetMapping("/parents")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsBomLineVO>> findParents(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String component) {
        return ApiResponse.success(service.findParents(cono, component));
    }

    @GetMapping("/children")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    public ApiResponse<List<BpcsBomLineVO>> expandChildren(
            @RequestParam(defaultValue = "001") String cono,
            @RequestParam String parent) {
        return ApiResponse.success(service.expandChildren(cono, parent));
    }
}
