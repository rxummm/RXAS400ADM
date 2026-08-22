package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.system.entity.Notification;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.vo.NotificationVO;
import com.rxas400adm.system.mapper.NotificationMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 站内通知（rx_notification）：告警/公告等事件按用户落库，
 * 并通过 WebSocket 个人队列 /user/{username}/queue/notifications 实时推送
 * （后端按目标用户定向，任何客户端均无法收到他人通知，S-01 修复）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    /** 个人通知队列（配合 /user 前缀 → /user/{username}/queue/notifications） */
    static final String NOTIFY_QUEUE = "/queue/notifications";

    private final NotificationMapper notificationMapper;
    private final SysUserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /** 给单个用户发通知（含实时推送） */
    
    public Notification send(String username, String type, String title, String content) {
        Notification notification = new Notification();
        notification.setUsername(username);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadFlag(0);
        notification.setCreatedTime(LocalDateTime.now());
        notificationMapper.insert(notification);
        broadcast(username, type, title, content);
        return notification;
    }

    /** 给全部活跃用户发通知（公告发布、系统告警等）：批量 INSERT + 逐用户定向推送 */
    
    public int sendToAllActiveUsers(String type, String title, String content) {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, "ACTIVE"));
        if (users.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Notification> notifications = users.stream().map(user -> {
            Notification notification = new Notification();
            notification.setUsername(user.getUsername());
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setReadFlag(0);
            notification.setCreatedTime(now);
            return notification;
        }).toList();
        // 单条批量 INSERT，替代 N 次单条 INSERT（B-13 修复）
        notificationMapper.insertBatch(notifications);
        // 按用户定向推送到个人队列（S-01 隔离）
        users.forEach(user -> broadcast(user.getUsername(), type, title, content));
        return users.size();
    }

    public PageResult<NotificationVO> mine(String username, int current, int size, boolean unreadOnly) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUsername, username);
        if (unreadOnly) {
            wrapper.eq(Notification::getReadFlag, 0);
        }
        wrapper.orderByDesc(Notification::getCreatedTime);
        Page<Notification> page = notificationMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(page.getTotal(),
                page.getRecords().stream().map(NotificationVO::from).toList());
    }

    public long unreadCount(String username) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUsername, username)
                .eq(Notification::getReadFlag, 0));
    }

    
    public void markRead(Long id, String username) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUsername, username)
                .set(Notification::getReadFlag, 1));
    }

    
    public void markAllRead(String username) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUsername, username)
                .eq(Notification::getReadFlag, 0)
                .set(Notification::getReadFlag, 1));
    }

    /** 删除单条通知（仅限本人的通知） */
    
    public void delete(Long id, String username) {
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUsername, username));
    }

    /** 批量删除通知（仅限本人的通知），返回实际删除条数 */
    
    public int deleteBatch(List<Long> ids, String username) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .in(Notification::getId, ids)
                .eq(Notification::getUsername, username));
    }

    private void broadcast(String username, String type, String title, String content) {
        try {
            // 按用户定向投递到个人队列：仅该用户在线会话可收到（S-01 隔离）
            messagingTemplate.convertAndSendToUser(username, NOTIFY_QUEUE, Map.of(
                    "username", username,
                    "type", type,
                    "title", title,
                    "content", content,
                    "time", LocalDateTime.now().toString()));
        } catch (Exception e) {
            // WebSocket 未初始化（如单测/启动早期）时忽略
        }
    }
}