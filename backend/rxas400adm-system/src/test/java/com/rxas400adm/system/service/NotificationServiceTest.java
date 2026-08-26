package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.system.entity.Notification;
import com.rxas400adm.system.entity.SysUser;
import com.rxas400adm.system.mapper.NotificationMapper;
import com.rxas400adm.system.mapper.SysUserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationMapper notificationMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private NotificationService service;

    @BeforeAll
    static void initTableInfo() {
        org.apache.ibatis.builder.MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Notification.class);
    }

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationMapper, userMapper, messagingTemplate);
    }

    /** 类型安全占位符：利用 any() 的目标类型推断消除裸 Class 字面量的 unchecked 转换警告 */
    private static LambdaQueryWrapper<SysUser> anyUserWrapper() {
        return any();
    }

    private static LambdaQueryWrapper<Notification> anyNotificationWrapper() {
        return any();
    }

    private static Wrapper<Notification> anyUpdateWrapper() {
        return any();
    }

    // ---------------- send ----------------

    @Test
    @DisplayName("send → 插入通知并推送到个人队列")
    void send_insertsAndBroadcasts() {
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(inv -> {
            inv.getArgument(0, Notification.class).setId(1L);
            return 1;
        });

        Notification result = service.send("alice", "ALERT", "告警标题", "内容");

        assertEquals("alice", result.getUsername());
        assertEquals("ALERT", result.getType());
        assertEquals(0, result.getReadFlag());
        verify(notificationMapper).insert(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("alice"), eq("/queue/notifications"), any());
    }

    // ---------------- sendToAllActiveUsers ----------------

    @Test
    @DisplayName("sendToAllActiveUsers → 无活跃用户返回 0")
    void sendToAllActiveUsers_noUsers_returnsZero() {
        when(userMapper.selectList(anyUserWrapper())).thenReturn(List.of());
        assertEquals(0, service.sendToAllActiveUsers("NOTICE", "t", "c"));
    }

    @Test
    @DisplayName("sendToAllActiveUsers → 批量插入并逐用户推送")
    void sendToAllActiveUsers_batchInsertAndBroadcast() {
        SysUser u1 = new SysUser();
        u1.setId(1L);
        u1.setUsername("alice");
        SysUser u2 = new SysUser();
        u2.setId(2L);
        u2.setUsername("bob");
        when(userMapper.selectList(anyUserWrapper())).thenReturn(List.of(u1, u2));
        when(notificationMapper.insertBatch(any())).thenReturn(2);

        int count = service.sendToAllActiveUsers("NOTICE", "t", "c");

        assertEquals(2, count);
        verify(notificationMapper).insertBatch(any());
        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), eq("/queue/notifications"), any());
    }

    // ---------------- markRead ----------------

    @Test
    @DisplayName("markRead → 更新 readFlag=1")
    void markRead_setsReadFlag() {
        when(notificationMapper.update(isNull(), anyUpdateWrapper())).thenReturn(1);
        service.markRead(10L, "alice");
        verify(notificationMapper).update(isNull(), anyUpdateWrapper());
    }

    // ---------------- markAllRead ----------------

    @Test
    @DisplayName("markAllRead → 批量更新 readFlag=1")
    void markAllRead() {
        when(notificationMapper.update(isNull(), anyUpdateWrapper())).thenReturn(3);
        service.markAllRead("alice");
        verify(notificationMapper).update(isNull(), anyUpdateWrapper());
    }

    // ---------------- delete ----------------

    @Test
    @DisplayName("delete → 按 ID + username 删除")
    void delete_byIdAndUsername() {
        when(notificationMapper.delete(anyNotificationWrapper())).thenReturn(1);
        service.delete(10L, "alice");
        verify(notificationMapper).delete(anyNotificationWrapper());
    }

    // ---------------- deleteBatch ----------------

    @Test
    @DisplayName("deleteBatch → 空列表返回 0")
    void deleteBatch_emptyList_returnsZero() {
        assertEquals(0, service.deleteBatch(List.of(), "alice"));
        assertEquals(0, service.deleteBatch(null, "alice"));
    }

    @Test
    @DisplayName("deleteBatch → 正常批量删除")
    void deleteBatch_normal() {
        when(notificationMapper.delete(anyNotificationWrapper())).thenReturn(2);
        assertEquals(2, service.deleteBatch(List.of(1L, 2L, 3L), "alice"));
    }

    // ---------------- unreadCount ----------------

    @Test
    @DisplayName("unreadCount → 返回未读数")
    void unreadCount() {
        when(notificationMapper.selectCount(anyNotificationWrapper())).thenReturn(5L);
        assertEquals(5L, service.unreadCount("alice"));
    }
}
