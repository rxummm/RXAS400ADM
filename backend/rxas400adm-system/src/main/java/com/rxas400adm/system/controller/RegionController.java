package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.RegionDTO;
import com.rxas400adm.system.service.IRegionService;
import com.rxas400adm.system.vo.RegionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 行政区划管理（rx_region）：懒加载子级树 + 分页/搜索 + CRUD。
 * 权限码：REGION_VIEW（查询）/ REGION_MANAGE（写操作）。
 */
@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
@Tag(name = "行政区划")
public class RegionController {

    private final IRegionService regionService;

    /** 下级行政区划（parentCode 空=省，配合前端懒加载树） */
    @GetMapping("/children")
    @PreAuthorize("hasAuthority('REGION_VIEW')")
    public ApiResponse<List<RegionVO>> children(@RequestParam(required = false) String parentCode) {
        return ApiResponse.success(regionService.children(parentCode).stream().map(RegionVO::from).toList());
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('REGION_VIEW')")
    public ApiResponse<PageResult<RegionVO>> page(@RequestParam(defaultValue = "1") int current,
                                                @RequestParam(defaultValue = "15") int size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer level,
                                                @RequestParam(required = false) String parentCode) {
        return ApiResponse.success(regionService.page(current, size, keyword, level, parentCode).map(RegionVO::from));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('REGION_VIEW')")
    public ApiResponse<List<RegionVO>> search(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer level) {
        return ApiResponse.success(regionService.search(keyword, level).stream().map(RegionVO::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REGION_MANAGE')")
    @OperateLog(module = "行政区划", operation = "新增行政区划")
    public ApiResponse<RegionVO> create(@Valid @RequestBody RegionDTO region) {
        return ApiResponse.success(RegionVO.from(regionService.create(region)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REGION_MANAGE')")
    @OperateLog(module = "行政区划", operation = "修改行政区划")
    public ApiResponse<RegionVO> update(@PathVariable Long id, @Valid @RequestBody RegionDTO dto) {
        return ApiResponse.success(RegionVO.from(regionService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REGION_MANAGE')")
    @OperateLog(module = "行政区划", operation = "删除行政区划")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        regionService.delete(id);
        return ApiResponse.success(null);
    }
}