package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.util.SecurityUtils;
import com.rxas400adm.system.dto.SysMenuDTO;
import com.rxas400adm.system.service.IMenuService;
import com.rxas400adm.system.vo.RequestableMenuVO;
import com.rxas400adm.system.vo.SysMenuVO;
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
 * 菜单管理（参照旧项目 SysMenuController）：rx_menu 表驱动动态菜单。
 * 支持菜单树查看 / 新增 / 编辑 / 删除 / 状态切换（显示·隐藏）。
 */
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Tag(name = "菜单管理")
public class SysMenuController {

    private final IMenuService menuService;

    /** 可申请菜单树（权限申请页：登录即可，排除 admin_only 与已拥有按钮） */
    @GetMapping("/requestable")
    public ApiResponse<List<RequestableMenuVO>> requestable() {
        String username = SecurityUtils.currentUsername();
        return ApiResponse.success(menuService.requestableMenuTree(username));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    public ApiResponse<List<SysMenuVO>> tree() {
        return ApiResponse.success(menuService.tree().stream().map(SysMenuVO::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    @OperateLog(module = "菜单管理", operation = "新增菜单")
    public ApiResponse<SysMenuVO> create(@Valid @RequestBody SysMenuDTO menu) {
        return ApiResponse.success(SysMenuVO.from(menuService.create(menu)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    @OperateLog(module = "菜单管理", operation = "修改菜单")
    public ApiResponse<SysMenuVO> update(@PathVariable Long id, @Valid @RequestBody SysMenuDTO menu) {
        return ApiResponse.success(SysMenuVO.from(menuService.update(id, menu)));
    }

    /** 状态切换（显示·隐藏）：status=1 显示 / 0 隐藏 */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    @OperateLog(module = "菜单管理", operation = "切换菜单状态")
    public ApiResponse<SysMenuVO> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        return ApiResponse.success(SysMenuVO.from(menuService.toggleStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MENU_MANAGE')")
    @OperateLog(module = "菜单管理", operation = "删除菜单")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.success(null);
    }
}
