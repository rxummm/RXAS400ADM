package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.model.UserProfileListRow;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.annotation.OperateLogModule;
import com.rxas400adm.common.annotation.OperateLogOperation;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户态管理：用户 profile 列表查询（分页）+ 用户态切换。
 */
@RestController
@RequestMapping("/api/v1/user-profiles")
@RequiredArgsConstructor
@Tag(name = "用户态管理")
public class UserProfileController {

    private final AS400ClientProvider clientProvider;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<PageResult<UserProfileListRow>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        current = (int) PageConstants.clampNum(current);
        size = (int) PageConstants.clampSize(size);
        return ApiResponse.success(clientProvider.current().listUserProfilesPaged(current, size));
    }

    @PostMapping("/switch")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @OperateLog(module = OperateLogModule.USER_PROFILE, operation = OperateLogOperation.SWITCH_USER_PROFILE)
    public ApiResponse<CommandResult> switchUser(@RequestParam String targetUser) {
        return ApiResponse.success(clientProvider.current().switchUser(targetUser));
    }
}
