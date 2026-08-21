package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.SysRoleDTO;
import com.rxas400adm.system.entity.SysRole;
import com.rxas400adm.system.service.IRoleService;
import com.rxas400adm.system.vo.SysRoleVO;
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
 * 角色管理（参照旧项目 SysRoleController）：
 * 角色 CRUD + 角色-菜单授权（menuIds），授权后非 admin 用户左侧菜单按角色裁剪。
 * GET / 供用户管理页角色分配下拉复用。
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理")
public class SysRoleController {

    private final IRoleService roleService;

    /** 全部角色（含已授权菜单 ID，用户管理页角色分配下拉用） */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<List<SysRoleVO>> list() {
        return ApiResponse.success(roleService.listAll().stream().map(SysRoleVO::from).toList());
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ApiResponse<PageResult<SysRoleVO>> page(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String keyword) {
        PageResult<SysRole> page = roleService.page(current, size, keyword);
        return ApiResponse.success(new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(SysRoleVO::from).toList()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @OperateLog(module = "角色管理", operation = "新增角色")
    public ApiResponse<SysRoleVO> create(@Valid @RequestBody SysRoleDTO role) {
        return ApiResponse.success(SysRoleVO.from(roleService.create(role)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @OperateLog(module = "角色管理", operation = "修改角色")
    public ApiResponse<SysRoleVO> update(@PathVariable Long id, @Valid @RequestBody SysRoleDTO role) {
        return ApiResponse.success(SysRoleVO.from(roleService.update(id, role)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @OperateLog(module = "角色管理", operation = "删除角色")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/batch-delete")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @OperateLog(module = "角色管理", operation = "批量删除角色")
    public ApiResponse<Integer> batchDelete(@Valid @RequestBody List<Long> ids) {
        return ApiResponse.success(roleService.batchDelete(ids));
    }
}