package com.rxas400adm.as400.collaboration;

import java.time.LocalDateTime;

public record CollaborationNotificationVO(
        Long id,
        Long collaborationId,
        String sender,
        String recipient,
        String message,
        String channel,
        Boolean isRead,
        LocalDateTime createdTime) {

    public static CollaborationNotificationVO from(CollaborationNotification e) {
        return new CollaborationNotificationVO(
                e.getId(), e.getCollaborationId(), e.getSender(), e.getRecipient(),
                e.getMessage(), e.getChannel(), e.getIsRead(), e.getCreatedTime());
    }
}
