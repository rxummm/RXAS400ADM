package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.model.SubsystemRow;
import com.rxas400adm.as400.service.ISubsystemService;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 系统服务管理（2.3.7）：子系统状态列表 + 启动/停止。
 */
@RestController
@RequestMapping("/api/v1/subsystems")
@RequiredArgsConstructor
@Tag(name = "子系统")
public class SubsystemController {

    private final ISubsystemService subsystemService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUBSYSTEM_VIEW')")
    public ApiResponse<List<SubsystemRow>> list() {
        return ApiResponse.success(subsystemService.list());
    }

    @PostMapping("/{name}/start")
    @PreAuthorize("hasAuthority('SUBSYSTEM_MANAGE')")
    @OperateLog(module = "系统服务", operation = "启动子系统")
    public ApiResponse<CommandResult> start(@PathVariable String name) {
        return ApiResponse.success(subsystemService.start(name));
    }

    @PostMapping("/{name}/end")
    @PreAuthorize("hasAuthority('SUBSYSTEM_MANAGE')")
    @OperateLog(module = "系统服务", operation = "停止子系统")
    public ApiResponse<CommandResult> end(@PathVariable String name) {
        return ApiResponse.success(subsystemService.end(name));
    }
}
