package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.OpTemplateCreateDTO;
import com.rxas400adm.as400.dto.OpTemplateUpdateDTO;
import com.rxas400adm.as400.service.IOpTemplateService;
import com.rxas400adm.as400.vo.OpTemplateVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.SecurityUtils;
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

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 操作模板/场景模式 Controller。
 */
@RestController
@RequestMapping("/api/v1/op-templates")
@RequiredArgsConstructor
@Tag(name = "操作模板")
public class OpTemplateController {

    private final IOpTemplateService opTemplateService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCRIPT_MANAGE')")
    public ApiResponse<PageResult<OpTemplateVO>> page(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "20") long size,
                                                      @RequestParam(required = false) String keyword) {
        return ApiResponse.success(opTemplateService.page(current, size, keyword));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCRIPT_MANAGE')")
    @OperateLog(module = "操作模板", operation = "新建模板")
    public ApiResponse<OpTemplateVO> create(@Valid @RequestBody OpTemplateCreateDTO dto) {
        return ApiResponse.success(opTemplateService.create(dto, SecurityUtils.currentUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCRIPT_MANAGE')")
    @OperateLog(module = "操作模板", operation = "更新模板")
    public ApiResponse<OpTemplateVO> update(@PathVariable Long id, @Valid @RequestBody OpTemplateUpdateDTO dto) {
        return ApiResponse.success(opTemplateService.update(id, dto, SecurityUtils.currentUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCRIPT_MANAGE')")
    @OperateLog(module = "操作模板", operation = "删除模板")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        opTemplateService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('SCRIPT_MANAGE')")
    @OperateLog(module = "操作模板", operation = "执行模板")
    public ApiResponse<Void> execute(@PathVariable Long id, @RequestParam Long serverId) {
        opTemplateService.execute(id, serverId);
        return ApiResponse.success();
    }
}
