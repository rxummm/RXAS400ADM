package com.rxas400adm.as400.collaboration;

import com.rxas400adm.common.annotation.OperateLog;
import com.rxas400adm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bpcs/order-collaboration")
@RequiredArgsConstructor
@Tag(name = "订单协同")
public class OrderCollaborationController {

    private final OrderCollaborationService collaborationService;

    // ==================== 协同管理 ====================

    @GetMapping
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "分页查询协同列表")
    public ApiResponse<Map<String, Object>> list(CollabQueryDTO query) {
        var page = collaborationService.list(query);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords().stream()
                .map(e -> OrderCollaborationVO.from(e, 0))
                .toList());
        result.put("total", page.getTotal());
        return ApiResponse.success(result);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "订单协同", operation = "创建协同")
    @Operation(summary = "创建订单协同")
    public ApiResponse<OrderCollaborationVO> create(@Valid @RequestBody CollabCreateDTO dto) {
        OrderCollaboration collab = collaborationService.create(dto);
        return ApiResponse.success(OrderCollaborationVO.from(collab, 0));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "订单协同", operation = "更新协同状态")
    @Operation(summary = "更新协同状态")
    public ApiResponse<OrderCollaborationVO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        OrderCollaboration collab = collaborationService.updateStatus(id, status);
        return ApiResponse.success(OrderCollaborationVO.from(collab, 0));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "订单协同", operation = "分配协同负责人")
    @Operation(summary = "分配协同负责人")
    public ApiResponse<OrderCollaborationVO> assign(@PathVariable Long id, @RequestParam String assignedTo) {
        OrderCollaboration collab = collaborationService.assign(id, assignedTo);
        return ApiResponse.success(OrderCollaborationVO.from(collab, 0));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "订单协同", operation = "删除协同")
    @Operation(summary = "删除协同记录")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        collaborationService.delete(id);
        return ApiResponse.success(null);
    }

    // ==================== 通知管理 ====================

    @PostMapping("/notifications")
    @PreAuthorize("hasAuthority('BPCS_MANAGE')")
    @OperateLog(module = "协同通知", operation = "发送通知")
    @Operation(summary = "发送协同通知")
    public ApiResponse<CollaborationNotificationVO> sendNotification(@Valid @RequestBody NotificationSendDTO dto) {
        return ApiResponse.success(CollaborationNotificationVO.from(
                collaborationService.sendNotification(dto)));
    }

    @GetMapping("/{collabId}/notifications")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "查询协同通知列表")
    public ApiResponse<List<CollaborationNotificationVO>> listNotifications(@PathVariable Long collabId) {
        return ApiResponse.success(collaborationService.listNotifications(collabId).stream()
                .map(CollaborationNotificationVO::from).toList());
    }

    @GetMapping("/my-notifications")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "查询我的通知")
    public ApiResponse<List<CollaborationNotificationVO>> myNotifications(@RequestParam String recipient) {
        return ApiResponse.success(collaborationService.listMyNotifications(recipient).stream()
                .map(CollaborationNotificationVO::from).toList());
    }

    @PutMapping("/notifications/{notifId}/read")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @OperateLog(module = "协同通知", operation = "标记通知已读")
    @Operation(summary = "标记通知已读")
    public ApiResponse<Void> markAsRead(@PathVariable Long notifId) {
        collaborationService.markAsRead(notifId);
        return ApiResponse.success(null);
    }

    @GetMapping("/notifications/unread-count")
    @PreAuthorize("hasAuthority('BPCS_VIEW')")
    @Operation(summary = "查询未读通知数")
    public ApiResponse<Integer> unreadCount(@RequestParam String recipient) {
        return ApiResponse.success(collaborationService.countUnread(recipient));
    }
}
