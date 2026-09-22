package com.rxas400adm.mrp.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.mrp.service.BomService;
import com.rxas400adm.mrp.vo.BomLineVO;
import com.rxas400adm.mrp.vo.BomMasterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mrp/bom")
@RequiredArgsConstructor
@Tag(name = "BOM管理", description = "物料清单查询")
public class BomController {

    private final BomService service;

    @GetMapping
    @PreAuthorize("hasAuthority('MRP_BOM_VIEW')")
    @Operation(summary = "分页查询BOM列表")
    public ApiResponse<PageResult<BomMasterVO>> page(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String parentItem,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.pageQuery(cono, parentItem, status, current, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MRP_BOM_VIEW')")
    @Operation(summary = "获取BOM详情")
    public ApiResponse<BomMasterVO> get(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @GetMapping("/{id}/lines")
    @PreAuthorize("hasAuthority('MRP_BOM_VIEW')")
    @Operation(summary = "获取BOM子件明细")
    public ApiResponse<List<BomLineVO>> getLines(@PathVariable Long id) {
        return ApiResponse.success(service.getLines(id));
    }

    @GetMapping("/expand/{parentItem}")
    @PreAuthorize("hasAuthority('MRP_BOM_VIEW')")
    @Operation(summary = "展开BOM表")
    public ApiResponse<List<BomLineVO>> expand(
            @PathVariable String parentItem,
            @RequestParam(defaultValue = "3") int maxLevels) {
        return ApiResponse.success(service.expandBom(parentItem, maxLevels));
    }
}