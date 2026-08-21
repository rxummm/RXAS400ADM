package com.rxas400adm.as400.controller;

import com.rxas400adm.as400.CommandResult;
import com.rxas400adm.as400.dto.IbmiSystemDTO;
import com.rxas400adm.as400.service.IIbmiSystemService;
import com.rxas400adm.as400.vo.IbmiSystemDetailVO;
import com.rxas400adm.as400.vo.IbmiSystemVO;
import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
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
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/as400")
@RequiredArgsConstructor
@Tag(name = "IBM i 服务器", description = "服务器注册 / 命令执行 / IFS 文件操作")
public class As400Controller {

    private final IIbmiSystemService systemService;

    /** N2：全局服务器列表（选择器/Dashboard/报表等所有已登录用户可见）。
     *  设计决策：不添加 @PreAuthorize——全局服务器列表为共享资源，所有操作员均需选择目标服务器。
     *  只返回 IbmiSystemVO——不暴露 username/passwordEncrypt 连接凭据；
     *  资产清单管理页需要完整凭据时走 /systems/detail（AS400_MANAGE）。 */
    @GetMapping("/systems")
    public ApiResponse<List<IbmiSystemVO>> list() {
        return ApiResponse.success(systemService.listVO());
    }

    /** N2：完整服务器列表（含 username 等连接凭据），仅供资产清单管理页（AS400_MANAGE） */
    @GetMapping("/systems/detail")
    @PreAuthorize("hasAuthority('AS400_MANAGE')")
    public ApiResponse<List<IbmiSystemDetailVO>> listDetail() {
        return ApiResponse.success(systemService.list().stream().map(IbmiSystemDetailVO::from).toList());
    }

    /** 启用的服务器列表（登录页 AS400 模式下拉框，免登录公开，参照旧项目 list-enabled）。
     *  P1-5 加固：仅返回最小视图（id/name/environment），不暴露 host/username/port。 */
    @GetMapping("/servers/enabled")
    public ApiResponse<List<com.rxas400adm.as400.vo.EnabledServerVO>> listEnabled() {
        return ApiResponse.success(systemService.listEnabled());
    }

    // P3-8：维护类写操作补审计（create/update/test 原先无 @OperateLog）
    @PostMapping("/systems")
    @PreAuthorize("hasAuthority('AS400_MANAGE')")
    @OperateLog(module = "AS400 管理", operation = "新增服务器")
    public ApiResponse<IbmiSystemDetailVO> create(@Valid @RequestBody IbmiSystemDTO system) {
        return ApiResponse.success(IbmiSystemDetailVO.from(systemService.create(system)));
    }

    @PutMapping("/systems/{id}")
    @PreAuthorize("hasAuthority('AS400_MANAGE')")
    @OperateLog(module = "AS400 管理", operation = "修改服务器")
    public ApiResponse<IbmiSystemDetailVO> update(@PathVariable Long id, @Valid @RequestBody IbmiSystemDTO system) {
        return ApiResponse.success(IbmiSystemDetailVO.from(systemService.update(id, system)));
    }

    @DeleteMapping("/systems/{id}")
    @PreAuthorize("hasAuthority('AS400_MANAGE')")
    @OperateLog(module = "AS400 管理", operation = "删除服务器")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        systemService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/systems/{id}/test")
    @PreAuthorize("hasAuthority('AS400_MANAGE')")
    @OperateLog(module = "AS400 管理", operation = "测试连接")
    public ApiResponse<CommandResult> test(@PathVariable Long id) {
        return ApiResponse.success(systemService.testConnection(id));
    }

    @PostMapping("/systems/{id}/command")
    @PreAuthorize("hasAuthority('AS400_MANAGE')")
    @OperateLog(module = "AS400 管理", operation = "执行 CL 命令")
    public ApiResponse<CommandResult> command(@PathVariable Long id, @RequestParam String command) {
        return ApiResponse.success(systemService.executeCommand(id, command));
    }
}