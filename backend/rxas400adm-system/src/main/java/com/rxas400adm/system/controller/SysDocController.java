package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.SecurityUtils;
import com.rxas400adm.system.dto.SysDocDTO;
import com.rxas400adm.system.service.ISysDocService;
import com.rxas400adm.system.vo.SysDocVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库管理（V66）：纯 DB 文档 CRUD，无 IFS 存储。
 * 查看 SYS_DOC_VIEW、编辑 SYS_DOC_MANAGE。
 */
@RestController
@RequestMapping("/api/v1/sys-docs")
@RequiredArgsConstructor
@Tag(name = "知识库", description = "知识库文档 CRUD")
public class SysDocController {

    private final ISysDocService sysDocService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYS_DOC_VIEW')")
    public ApiResponse<PageResult<SysDocVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(sysDocService.list(keyword, status, category, current, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SYS_DOC_VIEW')")
    public ApiResponse<SysDocVO> detail(@PathVariable Long id) {
        return ApiResponse.success(sysDocService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYS_DOC_MANAGE')")
    @OperateLog(module = "知识库", operation = "新建知识库文档")
    public ApiResponse<SysDocVO> create(@Valid @RequestBody SysDocDTO dto) {
        return ApiResponse.success(sysDocService.create(dto, SecurityUtils.currentUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYS_DOC_MANAGE')")
    @OperateLog(module = "知识库", operation = "更新知识库文档")
    public ApiResponse<SysDocVO> update(@PathVariable Long id, @Valid @RequestBody SysDocDTO dto) {
        return ApiResponse.success(sysDocService.update(id, dto, SecurityUtils.currentUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYS_DOC_MANAGE')")
    @OperateLog(module = "知识库", operation = "删除知识库文档")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        sysDocService.delete(id);
        return ApiResponse.success(null);
    }
}
