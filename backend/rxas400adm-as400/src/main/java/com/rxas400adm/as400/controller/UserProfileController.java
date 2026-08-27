package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.model.UserProfileListRow;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户态管理：用户 profile 列表查询 + 用户态切换。
 */
@RestController
@RequestMapping("/api/v1/user-profiles")
@RequiredArgsConstructor
@Tag(name = "用户态管理")
public class UserProfileController {

    private final AS400ClientProvider clientProvider;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<List<UserProfileListRow>> list() {
        return ApiResponse.success(clientProvider.current().listUserProfiles());
    }

    @PostMapping("/switch")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @OperateLog(module = "用户态管理", operation = "切换用户态")
    public ApiResponse<CommandResult> switchUser(@RequestParam String targetUser) {
        return ApiResponse.success(clientProvider.current().switchUser(targetUser));
    }
}
