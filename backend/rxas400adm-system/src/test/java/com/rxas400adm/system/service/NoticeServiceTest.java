package com.rxas400adm.system.service;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.dto.NoticeDTO;
import com.rxas400adm.system.entity.Notice;
import com.rxas400adm.system.mapper.NoticeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock private NoticeMapper noticeMapper;
    @Mock private INotificationService notificationService;

    private NoticeService service;

    @BeforeEach
    void setUp() {
        service = new NoticeService(noticeMapper, notificationService);
    }

    // ---------------- create ----------------

    @Test
    @DisplayName("create → 标题或内容为空则拒绝")
    void create_emptyTitleOrContent_shouldThrow() {
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle(" ");
        dto.setContent("content");
        assertThrows(BusinessException.class, () -> service.create(dto, "admin"));
    }

    @Test
    @DisplayName("create → 草稿(status=0)不推送通知")
    void create_draft_noNotification() {
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle("title");
        dto.setContent("content");
        dto.setStatus(0);
        when(noticeMapper.insert(any(Notice.class))).thenAnswer(inv -> {
            inv.getArgument(0, Notice.class).setId(1L);
            return 1;
        });

        Notice result = service.create(dto, "admin");

        assertEquals(0, result.getStatus());
        verify(notificationService, never()).sendToAllActiveUsers(any(), any(), any());
    }

    @Test
    @DisplayName("create → 发布(status=1)推送全体活跃用户")
    void create_published_notifiesAllActiveUsers() {
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle("公告");
        dto.setContent("内容");
        dto.setStatus(1);
        when(noticeMapper.insert(any(Notice.class))).thenAnswer(inv -> {
            inv.getArgument(0, Notice.class).setId(1L);
            return 1;
        });
        when(noticeMapper.updateById(any(Notice.class))).thenReturn(1);

        Notice result = service.create(dto, "admin");

        assertEquals(1, result.getStatus());
        assertNotNull(result.getPublishedTime());
        verify(notificationService).sendToAllActiveUsers("NOTICE", "公告", "内容");
    }

    @Test
    @DisplayName("create → status 为空默认为 1（发布）")
    void create_nullStatus_defaultsToPublished() {
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle("title");
        dto.setContent("content");
        when(noticeMapper.insert(any(Notice.class))).thenAnswer(inv -> {
            inv.getArgument(0, Notice.class).setId(1L);
            return 1;
        });
        when(noticeMapper.updateById(any(Notice.class))).thenReturn(1);

        Notice result = service.create(dto, "admin");
        assertEquals(1, result.getStatus());
        verify(notificationService).sendToAllActiveUsers(any(), any(), any());
    }

    // ---------------- update ----------------

    @Test
    @DisplayName("update → 不存在则抛 NOT_FOUND")
    void update_notFound_shouldThrow() {
        when(noticeMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.update(99L, new NoticeDTO()));
    }

    @Test
    @DisplayName("update → 状态从 0 切到 1 时触发推送")
    void update_statusChangeToPublished_triggersNotification() {
        Notice existing = new Notice();
        existing.setId(1L);
        existing.setStatus(0);
        existing.setTitle("old");
        when(noticeMapper.selectById(1L)).thenReturn(existing);
        when(noticeMapper.updateById(any(Notice.class))).thenReturn(1);

        NoticeDTO dto = new NoticeDTO();
        dto.setStatus(1);

        service.update(1L, dto);

        assertEquals(1, existing.getStatus());
        verify(notificationService).sendToAllActiveUsers("NOTICE", "old", existing.getContent());
    }

    @Test
    @DisplayName("update → 状态不变时不触发推送")
    void update_sameStatus_noNotification() {
        Notice existing = new Notice();
        existing.setId(1L);
        existing.setStatus(1);
        when(noticeMapper.selectById(1L)).thenReturn(existing);
        when(noticeMapper.updateById(any(Notice.class))).thenReturn(1);

        NoticeDTO dto = new NoticeDTO();
        dto.setStatus(1);
        service.update(1L, dto);

        verify(notificationService, never()).sendToAllActiveUsers(any(), any(), any());
    }

    // ---------------- delete ----------------

    @Test
    @DisplayName("delete → 不存在则抛 NOT_FOUND")
    void delete_notFound_shouldThrow() {
        when(noticeMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.delete(99L));
    }

    @Test
    @DisplayName("delete → 正常删除")
    void delete_success() {
        Notice n = new Notice();
        n.setId(1L);
        when(noticeMapper.selectById(1L)).thenReturn(n);
        when(noticeMapper.deleteById(1L)).thenReturn(1);
        service.delete(1L);
        verify(noticeMapper).deleteById(1L);
    }
}
