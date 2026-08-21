package com.rxas400adm.system.service;

import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.entity.Notification;

import java.util.List;

/**
 * 站内通知服务接口（rx_notification）。
 */
public interface INotificationService {

    Notification send(String username, String type, String title, String content);

    int sendToAllActiveUsers(String type, String title, String content);

    PageResult<Notification> mine(String username, int current, int size, boolean unreadOnly);

    long unreadCount(String username);

    void markRead(Long id, String username);

    void markAllRead(String username);

    void delete(Long id, String username);

    int deleteBatch(List<Long> ids, String username);
}
