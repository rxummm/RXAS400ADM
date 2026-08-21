package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.model.AuthorityRow;
import com.rxas400adm.as400.model.ObjectDetail;
import com.rxas400adm.as400.model.ObjectRefRow;
import com.rxas400adm.as400.model.ObjectRow;
import com.rxas400adm.as400.service.IObjectService;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 资产管理：对象搜索（DSPOBJD）/ 详情 / 引用分析（DSPPGMREF），按库/类型过滤，
 * 搜索接口支持关键字 + 后端分页（大库避免全量传输）。
 */
@RestController
@RequestMapping("/api/v1/objects")
@RequiredArgsConstructor
@Tag(name = "对象管理")
public class ObjectController {

    private final IObjectService objectService;

    @GetMapping
    @PreAuthorize("hasAuthority('OBJECT_VIEW')")
    public ApiResponse<PageResult<ObjectRow>> search(
            @RequestParam(required = false) String library,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        int page = Math.max(1, current);
        int pageSize = Math.max(1, Math.min(size, 200));
        return ApiResponse.success(objectService.searchObjects(library, type, keyword, page, pageSize));
    }

    @GetMapping("/{library}/{name}/detail")
    @PreAuthorize("hasAuthority('OBJECT_VIEW')")
    public ApiResponse<ObjectDetail> detail(@PathVariable String library, @PathVariable String name) {
        return ApiResponse.success(objectService.objectDetail(library, name));
    }

    @GetMapping("/{library}/{name}/references")
    @PreAuthorize("hasAuthority('OBJECT_VIEW')")
    public ApiResponse<List<ObjectRefRow>> references(@PathVariable String library,
                                                      @PathVariable String name,
                                                      @RequestParam(defaultValue = "IN") String direction) {
        return ApiResponse.success(objectService.objectReferences(library, name, direction));
    }

    @GetMapping("/{library}/{name}/authorities")
    @PreAuthorize("hasAuthority('OBJECT_VIEW')")
    public ApiResponse<List<AuthorityRow>> authorities(@PathVariable String library,
                                                       @PathVariable String name) {
        return ApiResponse.success(objectService.objectAuthorities(library, name));
    }
}
