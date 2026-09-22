package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.dto.UserProfileBatchDeleteDTO;
import com.rxas400adm.as400.dto.UserProfileCreateDTO;
import com.rxas400adm.as400.dto.UserProfileUpdateDTO;
import com.rxas400adm.as400.entity.UserProfileLog;
import com.rxas400adm.as400.model.UserProfileListRow;
import com.rxas400adm.as400.service.IUserProfileService;
import com.rxas400adm.as400.vo.UserProfileBatchDeleteResultVO;
import com.rxas400adm.as400.vo.UserProfileCreateResult;
import com.rxas400adm.as400.vo.UserProfileDeleteStatsVO;
import com.rxas400adm.as400.vo.UserProfileDetailVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.annotation.OperateLogModule;
import com.rxas400adm.common.annotation.OperateLogOperation;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "获取用户Profile列表（分页）")
    public ApiResponse<PageResult<UserProfileListRow>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(userProfileService.listUserProfiles(current, size, keyword));
    }

    @GetMapping("/{userName}")
    @PreAuthorize("hasAuthority('USER_PROFILE_VIEW')")
    @Operation(summary = "获取用户Profile详情")
    public ApiResponse<UserProfileDetailVO> getDetail(@PathVariable String userName) {
        return ApiResponse.success(userProfileService.getUserProfile(userName));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_PROFILE_CREATE')")
    @OperateLog(module = OperateLogModule.AS400_USER_PROFILE, operation = OperateLogOperation.CREATE_AS400_USER_PROFILE)
    @Operation(summary = "创建用户Profile")
    public ApiResponse<UserProfileCreateResult> create(
            @Valid @RequestBody UserProfileCreateDTO dto) {
        return ApiResponse.success(userProfileService.createUserProfile(dto, SecurityUtils.currentUsername()));
    }

    @PutMapping("/{userName}")
    @PreAuthorize("hasAuthority('USER_PROFILE_UPDATE')")
    @OperateLog(module = OperateLogModule.AS400_USER_PROFILE, operation = OperateLogOperation.UPDATE_AS400_USER_PROFILE)
    @Operation(summary = "更新用户Profile")
    public ApiResponse<Void> update(
            @PathVariable String userName,
            @Valid @RequestBody UserProfileUpdateDTO dto) {
        userProfileService.updateUserProfile(userName, dto, SecurityUtils.currentUsername());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{userName}")
    @PreAuthorize("hasAuthority('USER_PROFILE_DELETE')")
    @OperateLog(module = OperateLogModule.AS400_USER_PROFILE, operation = OperateLogOperation.DELETE_AS400_USER_PROFILE)
    @Operation(summary = "删除用户Profile")
    public ApiResponse<Void> delete(
            @PathVariable String userName,
            @RequestParam(required = false) String deleteReason,
            @RequestParam(required = false) String deletionType) {
        userProfileService.deleteUserProfile(userName, SecurityUtils.currentUsername(), deleteReason, deletionType);
        return ApiResponse.success(null);
    }

    @GetMapping("/{userName}/logs")
    @PreAuthorize("hasAuthority('USER_PROFILE_VIEW')")
    @Operation(summary = "获取用户Profile操作日志")
    public ApiResponse<PageResult<UserProfileLog>> getLogs(
            @PathVariable String userName,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(userProfileService.getProfileLogs(userName, current, size));
    }

    @PostMapping("/batch-delete")
    @PreAuthorize("hasAuthority('USER_PROFILE_DELETE')")
    @OperateLog(module = OperateLogModule.AS400_USER_PROFILE, operation = "BATCH_DELETE_AS400_USER_PROFILE")
    @Operation(summary = "批量删除用户Profile")
    public ApiResponse<UserProfileBatchDeleteResultVO> batchDelete(
            @Valid @RequestBody UserProfileBatchDeleteDTO dto) {
        return ApiResponse.success(userProfileService.batchDeleteUserProfiles(dto, SecurityUtils.currentUsername()));
    }

    @GetMapping("/delete-stats")
    @PreAuthorize("hasAuthority('USER_PROFILE_VIEW')")
    @Operation(summary = "获取用户Profile删除统计")
    public ApiResponse<UserProfileDeleteStatsVO> getDeleteStats() {
        return ApiResponse.success(userProfileService.getDeleteStats());
    }
}
