package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.UserProfileCreateDTO;
import com.rxas400adm.as400.dto.UserProfileUpdateDTO;
import com.rxas400adm.as400.model.UserProfileListRow;
import com.rxas400adm.as400.service.IUserProfileService;
import com.rxas400adm.as400.vo.UserProfileCreateResult;
import com.rxas400adm.as400.vo.UserProfileDetailVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AS400用户Profile管理Controller
 */
@RestController
@RequestMapping("/api/v1/as400/user-profiles")
@RequiredArgsConstructor
@Tag(name = "AS400用户Profile管理")
public class UserProfileManagementController {

    private final IUserProfileService userProfileService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_PROFILE_VIEW')")
    @Operation(summary = "获取用户Profile列表")
    public ApiResponse<List<UserProfileListRow>> list() {
        return ApiResponse.success(userProfileService.listUserProfiles());
    }

    @GetMapping("/{userName}")
    @PreAuthorize("hasAuthority('USER_PROFILE_VIEW')")
    @Operation(summary = "获取用户Profile详情")
    public ApiResponse<UserProfileDetailVO> getDetail(@PathVariable String userName) {
        return ApiResponse.success(userProfileService.getUserProfile(userName));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_PROFILE_CREATE')")
    @OperateLog(module = "AS400用户管理", operation = "创建用户Profile")
    @Operation(summary = "创建用户Profile")
    public ApiResponse<UserProfileCreateResult> create(
            @Valid @RequestBody UserProfileCreateDTO dto) {
        return ApiResponse.success(userProfileService.createUserProfile(dto, SecurityUtils.currentUsername()));
    }

    @PutMapping("/{userName}")
    @PreAuthorize("hasAuthority('USER_PROFILE_UPDATE')")
    @OperateLog(module = "AS400用户管理", operation = "更新用户Profile")
    @Operation(summary = "更新用户Profile")
    public ApiResponse<Void> update(
            @PathVariable String userName,
            @Valid @RequestBody UserProfileUpdateDTO dto) {
        userProfileService.updateUserProfile(userName, dto, SecurityUtils.currentUsername());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{userName}")
    @PreAuthorize("hasAuthority('USER_PROFILE_DELETE')")
    @OperateLog(module = "AS400用户管理", operation = "删除用户Profile")
    @Operation(summary = "删除用户Profile")
    public ApiResponse<Void> delete(
            @PathVariable String userName) {
        userProfileService.deleteUserProfile(userName, SecurityUtils.currentUsername());
        return ApiResponse.success(null);
    }
}
