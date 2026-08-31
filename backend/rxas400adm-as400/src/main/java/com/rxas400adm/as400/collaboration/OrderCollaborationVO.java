package com.rxas400adm.as400.collaboration;

import java.time.LocalDate;

public record OrderCollaborationVO(
        Long id,
        String orderNo,
        String customerCode,
        String customerName,
        String status,
        String priority,
        String assignedTo,
        LocalDate dueDate,
        String notes,
        int unreadNotifications) {

    public static OrderCollaborationVO from(OrderCollaboration e, int unreadCount) {
        return new OrderCollaborationVO(
                e.getId(), e.getOrderNo(), e.getCustomerCode(), e.getCustomerName(),
                e.getStatus(), e.getPriority(), e.getAssignedTo(), e.getDueDate(),
                e.getNotes(), unreadCount);
    }
}
