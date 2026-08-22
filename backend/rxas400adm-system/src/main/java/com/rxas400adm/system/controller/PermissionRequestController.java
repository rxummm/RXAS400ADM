package com.rxas400adm.system.controller;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.dto.PermissionRequestCreateDTO;
import com.rxas400adm.system.service.IPermissionRequestService;
import com.rxas400adm.system.vo.PermissionRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rxas400adm.system.vo.PendingCountVO;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 权限自助申请（rx_permission_request）：登录用户可申请（菜单树勾选或权限码）；审批需 SYS_PERMISSION_REQUEST。
 * 参照旧项目：申请菜单/按钮 → 审批通过写入用户直接授权（rx_user_menu）。
 */
@RestController
@RequestMapping("/api/v1/permission-requests")
@RequiredArgsConstructor
@Tag(name = "权限申请")
public class PermissionRequestController {

    private final IPermissionRequestService requestService;

    /**
     * 提交申请（登录即可）：支持菜单树模式 {menuIds, menuNames, reason} 与权限码模式 {permissionCode, reason}。
     */
    @PostMapping
    @OperateLog(module = "权限申请", operation = "提交权限申请")
    public ApiResponse<PermissionRequestVO> create(@Valid @RequestBody PermissionRequestCreateDTO dto) {
        return ApiResponse.success(PermissionRequestVO.from(requestService.create(
                currentUsername(), dto.getPermissionCode(), dto.getMenuIds(), dto.getMenuNames(), dto.getReason())));
    }

    /** 我的申请 */
    @GetMapping("/mine")
    public ApiResponse<PageResult<PermissionRequestVO>> mine(@RequestParam(defaultValue = "1") int current,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(requestService.mine(currentUsername(), current, size).map(PermissionRequestVO::from));
    }

    /** 管理端：全部申请（可按状态/关键字过滤） */
    @GetMapping
    @PreAuthorize("hasAuthority('SYS_PERMISSION_REQUEST')")
    public ApiResponse<PageResult<PermissionRequestVO>> adminPage(@RequestParam(defaultValue = "1") int current,
                                                                @RequestParam(defaultValue = "20") int size,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(required = false) String keyword) {
        return ApiResponse.success(requestService.adminPage(current, size, status, keyword).map(PermissionRequestVO::from));
    }

    /** 待审批数量（角标） */
    @GetMapping("/pending-count")
    @PreAuthorize("hasAuthority('SYS_PERMISSION_REQUEST')")
    public ApiResponse<PendingCountVO> pendingCount() {
        return ApiResponse.success(new PendingCountVO(requestService.pendingCount()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('SYS_PERMISSION_REQUEST')")
    @OperateLog(module = "权限申请", operation = "审批通过权限申请")
    public ApiResponse<PermissionRequestVO> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.success(PermissionRequestVO.from(requestService.approve(id, currentUsername(),
                body == null ? null : body.get("comment"))));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('SYS_PERMISSION_REQUEST')")
    @OperateLog(module = "权限申请", operation = "驳回权限申请")
    public ApiResponse<PermissionRequestVO> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.success(PermissionRequestVO.from(requestService.reject(id, currentUsername(),
                body == null ? null : body.get("comment"))));
    }

    private String currentUsername() {
        return com.rxas400adm.common.util.SecurityUtils.currentUsername();
    }
}