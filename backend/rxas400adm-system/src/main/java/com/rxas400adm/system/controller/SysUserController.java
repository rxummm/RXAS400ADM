package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.UserDTO;
import com.rxas400adm.system.dto.UserMenuUpdateDTO;
import com.rxas400adm.system.dto.UserUpdateDTO;
import com.rxas400adm.system.service.SysUserService;
import com.rxas400adm.system.service.IUserMenuService;
import com.rxas400adm.system.vo.SysMenuVO;
import com.rxas400adm.system.vo.UserVO;
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

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户 CRUD / 角色分配 / 密码管理")
public class SysUserController {

    private final SysUserService userService;
    private final IUserMenuService userMenuService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword) {
        return ApiResponse.success(userService.page(current, size, keyword));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<UserVO> create(@Valid @RequestBody UserDTO dto) {
        return ApiResponse.success(userService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return ApiResponse.success(userService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @OperateLog(module = "用户管理", operation = "删除用户")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }

    // ==================== 用户-菜单直接授权（参照旧项目 SysPermissionManage） ====================

    /** 用户已有菜单 ID（角色授权 ∪ 直接授权） */
    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<Set<Long>> userMenuIds(@PathVariable Long id) {
        return ApiResponse.success(userMenuService.getUserMenuIds(id));
    }

    /** 用户直接授权 ID（仅 rx_user_menu，供当前权限 Tab 回显） */
    @GetMapping("/{id}/menus/direct")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<Set<Long>> userDirectMenuIds(@PathVariable Long id) {
        return ApiResponse.success(userMenuService.getUserDirectMenuIds(id));
    }

    /** 可分配权限树（管理员授权弹窗，排除已拥有菜单页，目录/按钮保留占位） */
    @GetMapping("/{id}/menus/manageable-tree")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<List<SysMenuVO>> manageableTree(@PathVariable Long id) {
        return ApiResponse.success(userMenuService.getManageableMenuTree(id).stream().map(SysMenuVO::from).toList());
    }

    /** 勾选授权（追加，幂等） */
    @PostMapping("/{id}/menus/add")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @OperateLog(module = "用户管理", operation = "用户菜单授权")
    public ApiResponse<Void> addUserMenus(@PathVariable Long id, @Valid @RequestBody UserMenuUpdateDTO body) {
        userMenuService.addUserMenus(id, body.getMenuIds());
        return ApiResponse.success(null);
    }

    /** 移除授权（目录/菜单页连带子孙） */
    @PostMapping("/{id}/menus/remove")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @OperateLog(module = "用户管理", operation = "移除用户菜单授权")
    public ApiResponse<Void> removeUserMenus(@PathVariable Long id, @Valid @RequestBody UserMenuUpdateDTO body) {
        userMenuService.removeUserMenus(id, body.getMenuIds());
        return ApiResponse.success(null);
    }


}