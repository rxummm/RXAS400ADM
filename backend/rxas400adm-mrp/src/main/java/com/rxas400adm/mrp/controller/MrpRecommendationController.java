package com.rxas400adm.mrp.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.annotation.OperateLogModule;
import com.rxas400adm.common.annotation.OperateLogOperation;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.mrp.service.MrpRecommendationService;
import com.rxas400adm.mrp.vo.MrpRecommendationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mrp/recommendations")
@RequiredArgsConstructor
@Tag(name = "MRP建议管理", description = "物料需求计划建议")
public class MrpRecommendationController {

    private final MrpRecommendationService service;

    @GetMapping
    @PreAuthorize("hasAuthority('MRP_RECOMMENDATION_VIEW')")
    @Operation(summary = "分页查询建议列表")
    public ApiResponse<PageResult<MrpRecommendationVO>> page(
            @RequestParam(required = false) String cono,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String recommendType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(service.pageQuery(cono, itemCode, recommendType, status, current, size));
    }

    @PutMapping("/{id}/release")
    @PreAuthorize("hasAuthority('MRP_RECOMMENDATION_RELEASE')")
    @OperateLog(module = OperateLogModule.MRP, operation = OperateLogOperation.RELEASE_RECOMMENDATION)
    @Operation(summary = "释放MRP建议")
    public ApiResponse<Void> release(@PathVariable Long id) {
        service.release(id);
        return ApiResponse.success();
    }
}