package com.rxas400adm.as400.collaboration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.util.EntityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCollaborationService {

    private final OrderCollaborationMapper collabMapper;
    private final CollaborationNotificationMapper notificationMapper;

    public Page<OrderCollaboration> list(CollabQueryDTO query) {
        Page<OrderCollaboration> page = new Page<>(query.getAdjustedCurrent(), query.getAdjustedSize());
        LambdaQueryWrapper<OrderCollaboration> qw = new LambdaQueryWrapper<OrderCollaboration>()
                .like(query.orderNo() != null && !query.orderNo().isBlank(),
                        OrderCollaboration::getOrderNo, query.orderNo())
                .like(query.customerCode() != null && !query.customerCode().isBlank(),
                        OrderCollaboration::getCustomerCode, query.customerCode())
                .eq(query.status() != null && !query.status().isBlank(),
                        OrderCollaboration::getStatus, query.status())
                .eq(query.priority() != null && !query.priority().isBlank(),
                        OrderCollaboration::getPriority, query.priority())
                .like(query.assignedTo() != null && !query.assignedTo().isBlank(),
                        OrderCollaboration::getAssignedTo, query.assignedTo())
                .ge(query.dueDateFrom() != null, OrderCollaboration::getDueDate, query.dueDateFrom())
                .le(query.dueDateTo() != null, OrderCollaboration::getDueDate, query.dueDateTo())
                .orderByDesc(OrderCollaboration::getCreatedTime);
        return collabMapper.selectPage(page, qw);
    }

    public OrderCollaboration create(CollabCreateDTO dto) {
        OrderCollaboration collab = new OrderCollaboration();
        collab.setOrderNo(dto.orderNo());
        collab.setCustomerCode(dto.customerCode());
        collab.setCustomerName(dto.customerName());
        collab.setStatus("PENDING");
        collab.setPriority(dto.priority() != null ? dto.priority() : "NORMAL");
        collab.setAssignedTo(dto.assignedTo());
        collab.setDueDate(dto.dueDate());
        collab.setNotes(dto.notes());
        collabMapper.insert(collab);
        return collab;
    }

    public OrderCollaboration updateStatus(Long id, String status) {
        OrderCollaboration collab = EntityUtil.require(id, "订单协同", collabMapper::selectById);
        if (!List.of("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED").contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法状态: " + status);
        }
        collab.setStatus(status);
        collabMapper.updateById(collab);
        return collab;
    }

    public OrderCollaboration assign(Long id, String assignedTo) {
        OrderCollaboration collab = EntityUtil.require(id, "订单协同", collabMapper::selectById);
        collab.setAssignedTo(assignedTo);
        collabMapper.updateById(collab);
        return collab;
    }

    public void delete(Long id) {
        EntityUtil.require(id, "订单协同", collabMapper::selectById);
        notificationMapper.delete(new LambdaQueryWrapper<CollaborationNotification>()
                .eq(CollaborationNotification::getCollaborationId, id));
        collabMapper.deleteById(id);
    }

    // ==================== 通知 ====================

    public CollaborationNotification sendNotification(NotificationSendDTO dto) {
        EntityUtil.require(dto.collaborationId(), "订单协同", collabMapper::selectById);
        CollaborationNotification notif = new CollaborationNotification();
        notif.setCollaborationId(dto.collaborationId());
        notif.setSender(dto.sender());
        notif.setRecipient(dto.recipient());
        notif.setMessage(dto.message());
        notif.setChannel(dto.channel() != null ? dto.channel() : "SYSTEM");
        notif.setIsRead(false);
        notificationMapper.insert(notif);
        return notif;
    }

    public List<CollaborationNotification> listNotifications(Long collaborationId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<CollaborationNotification>()
                .eq(CollaborationNotification::getCollaborationId, collaborationId)
                .orderByDesc(CollaborationNotification::getCreatedTime));
    }

    public List<CollaborationNotification> listMyNotifications(String recipient) {
        return notificationMapper.selectList(new LambdaQueryWrapper<CollaborationNotification>()
                .eq(CollaborationNotification::getRecipient, recipient)
                .orderByDesc(CollaborationNotification::getCreatedTime)
                .last("LIMIT 50"));
    }

    public void markAsRead(Long notificationId) {
        CollaborationNotification notif = EntityUtil.require(notificationId, "协同通知", notificationMapper::selectById);
        notif.setIsRead(true);
        notificationMapper.updateById(notif);
    }

    public int countUnread(String recipient) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<CollaborationNotification>()
                .eq(CollaborationNotification::getRecipient, recipient)
                .eq(CollaborationNotification::getIsRead, false)).intValue();
    }
}
