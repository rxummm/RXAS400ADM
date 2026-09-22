package com.rxas400adm.cost.controller;

import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.cost.service.CostCollectionService;
import com.rxas400adm.cost.vo.CostCollectionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cost/collections")
@RequiredArgsConstructor
@Tag(name = "成本归集", description = "成本归集数据管理")
public class CostCollectionController {

    private final CostCollectionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('COST_COLLECTION_VIEW')")
    @Operation(summary = "分页查询成本归集")
    public ApiResponse<PageResult<CostCollectionVO>> page(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String costType,
            @RequestParam(required = false) String costObjectType,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.pageQuery(cono, costType, costObjectType, period, status, current, size)
                .map(CostCollectionVO::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COST_COLLECTION_VIEW')")
    @Operation(summary = "获取成本归集详情")
    public ApiResponse<CostCollectionVO> get(@PathVariable Long id) {
        return ApiResponse.success(CostCollectionVO.from(service.getById(id)));
    }

    @GetMapping("/by-period/{period}")
    @PreAuthorize("hasAuthority('COST_COLLECTION_VIEW')")
    @Operation(summary = "按期间查询成本归集")
    public ApiResponse<List<CostCollectionVO>> listByPeriod(@PathVariable String period) {
        return ApiResponse.success(service.listByPeriod(period).stream().map(CostCollectionVO::from).toList());
    }
}