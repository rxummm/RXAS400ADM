package com.rxas400adm.system.controller;
import com.rxas400adm.common.util.SecurityUtils;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.service.INotificationService;
import com.rxas400adm.system.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rxas400adm.system.dto.BatchDeleteDTO;
import com.rxas400adm.system.vo.BatchDeleteResultVO;
import com.rxas400adm.system.vo.UnreadCountVO;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 站内通知（rx_notification）：当前用户的未读角标 / 分页列表 / 已读 / 删除。
 * 登录即可查看；删除需权限码 NOTIFICATION_MANAGE。
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "通知中心")
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping("/mine")
    public ApiResponse<PageResult<NotificationVO>> mine(@RequestParam(defaultValue = "1") int current,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return ApiResponse.success(notificationService.mine(SecurityUtils.currentUsername(), current, size, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountVO> unreadCount() {
        return ApiResponse.success(new UnreadCountVO(notificationService.unreadCount(SecurityUtils.currentUsername())));
    }

    @PostMapping("/{id}/read")
    @OperateLog(module = "通知中心", operation = "标记已读")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, SecurityUtils.currentUsername());
        return ApiResponse.success(null);
    }

    @PostMapping("/read-all")
    @OperateLog(module = "通知中心", operation = "全部标记已读")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.currentUsername());
        return ApiResponse.success(null);
    }

    /** 删除单条通知（仅限本人，需 NOTIFICATION_MANAGE） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_MANAGE')")
    @OperateLog(module = "通知中心", operation = "删除通知")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        notificationService.delete(id, SecurityUtils.currentUsername());
        return ApiResponse.success(null);
    }

    /** 批量删除通知（仅限本人，需 NOTIFICATION_MANAGE） */
    @PostMapping("/batch-delete")
    @PreAuthorize("hasAuthority('NOTIFICATION_MANAGE')")
    @OperateLog(module = "通知中心", operation = "批量删除通知")
    public ApiResponse<BatchDeleteResultVO> batchDelete(@Valid @RequestBody BatchDeleteDTO body) {
        int deleted = notificationService.deleteBatch(body.getIds(), SecurityUtils.currentUsername());
        return ApiResponse.success(new BatchDeleteResultVO(deleted));
    }
}
