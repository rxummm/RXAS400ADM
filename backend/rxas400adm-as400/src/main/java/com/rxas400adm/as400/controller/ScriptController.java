package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.dto.CommandScriptRequest;
import com.rxas400adm.as400.service.ICommandScriptService;
import com.rxas400adm.as400.vo.CommandScriptVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 命令脚本中心（2.4.3）：CL 命令保存/复用、收藏、标签分类、一键执行。
 */
@RestController
@RequestMapping("/api/v1/scripts")
@RequiredArgsConstructor
@Tag(name = "脚本管理")
public class ScriptController {

    private final ICommandScriptService scriptService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCRIPT_VIEW')")
    public ApiResponse<List<CommandScriptVO>> list(@RequestParam(required = false) Boolean favorite,
                                                   @RequestParam(required = false) String tag) {
        return ApiResponse.success(scriptService.list(favorite, tag).stream().map(CommandScriptVO::from).toList());
    }

    @GetMapping("/tags")
    @PreAuthorize("hasAuthority('SCRIPT_VIEW')")
    public ApiResponse<List<String>> tags() {
        return ApiResponse.success(scriptService.tags());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCRIPT_MANAGE')")
    @OperateLog(module = "命令脚本", operation = "新建脚本")
    public ApiResponse<CommandScriptVO> create(@Valid @RequestBody CommandScriptRequest request) {
        return ApiResponse.success(CommandScriptVO.from(scriptService.create(request, currentUsername())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCRIPT_MANAGE')")
    @OperateLog(module = "命令脚本", operation = "更新脚本")
    public ApiResponse<CommandScriptVO> update(@PathVariable Long id,
                                               @Valid @RequestBody CommandScriptRequest request) {
        return ApiResponse.success(CommandScriptVO.from(scriptService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCRIPT_MANAGE')")
    @OperateLog(module = "命令脚本", operation = "删除脚本")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        scriptService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/favorite")
    @PreAuthorize("hasAuthority('SCRIPT_MANAGE')")
    @OperateLog(module = "命令脚本", operation = "切换收藏状态")
    public ApiResponse<CommandScriptVO> favorite(@PathVariable Long id, @RequestParam Boolean favorite) {
        return ApiResponse.success(CommandScriptVO.from(scriptService.toggleFavorite(id, favorite)));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('SCRIPT_MANAGE')")
    @OperateLog(module = "命令脚本", operation = "执行脚本")
    public ApiResponse<CommandResult> execute(@PathVariable Long id, @RequestParam Long serverId) {
        return ApiResponse.success(scriptService.execute(id, serverId));
    }

    private String currentUsername() {
        return com.rxas400adm.common.util.SecurityUtils.currentUsername();
    }
}
