package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.SysPermissionDTO;
import com.rxas400adm.system.service.IPermissionManageService;
import com.rxas400adm.system.vo.PermissionVO;
import com.rxas400adm.system.vo.SysPermissionVO;
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
 * 权限码管理（rx_permission CRUD + 菜单匹配下拉建议）。
 * - 权限码 = 后端 @PreAuthorize 与前端门控的「钥匙」，统一在此注册维护
 * - suggest 供菜单管理页 perms 下拉选择（按菜单业务域过滤，杜绝手填拼写错误）
 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "权限管理")
public class PermissionController {

    private final IPermissionManageService permissionService;

    /** 分页查询 */
    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public ApiResponse<PageResult<PermissionVO>> page(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "20") long size,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String module) {
        return ApiResponse.success(permissionService.page(keyword, module, current, size));
    }

    /** 全部权限码（下拉字典用，登录即可） */
    @GetMapping("/all")
    public ApiResponse<List<PermissionVO>> all() {
        return ApiResponse.success(permissionService.listAll());
    }

    /** 按菜单业务域过滤的建议码（菜单管理页 perms 下拉） */
    @GetMapping("/suggest")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    public ApiResponse<List<PermissionVO>> suggest(@RequestParam(required = false) String menuTitle,
                                                   @RequestParam(required = false) String keyword) {
        return ApiResponse.success(permissionService.suggest(menuTitle, keyword));
    }

    /** 新增 */
    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    @OperateLog(module = "权限管理", operation = "新增权限")
    public ApiResponse<SysPermissionVO> create(@Valid @RequestBody SysPermissionDTO dto) {
        return ApiResponse.success(SysPermissionVO.from(permissionService.create(dto)));
    }

    /** 更新 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    @OperateLog(module = "权限管理", operation = "更新权限")
    public ApiResponse<SysPermissionVO> update(@PathVariable Long id, @Valid @RequestBody SysPermissionDTO dto) {
        return ApiResponse.success(SysPermissionVO.from(permissionService.update(id, dto)));
    }

    /** 删除（被菜单/角色绑定引用则拒绝） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    @OperateLog(module = "权限管理", operation = "删除权限")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.success(null);
    }
}