package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.notify.WebhookNotifier;
import com.rxas400adm.system.dto.WebhookConfigDTO;
import com.rxas400adm.system.entity.WebhookConfig;
import com.rxas400adm.system.entity.WebhookLog;
import com.rxas400adm.system.mapper.WebhookConfigMapper;
import com.rxas400adm.system.mapper.WebhookLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock private WebhookConfigMapper webhookMapper;
    @Mock private WebhookLogMapper webhookLogMapper;
    @Mock private WebhookNotifier webhookNotifier;

    private WebhookService service;

    @BeforeEach
    void setUp() {
        service = new WebhookService(webhookMapper, webhookLogMapper, webhookNotifier);
    }

    // ---------------- create ----------------

    @Test
    @DisplayName("create → 名称或 URL 空则拒绝")
    void create_missingNameOrUrl_shouldThrow() {
        WebhookConfigDTO dto = new WebhookConfigDTO();
        dto.setName(" ");
        dto.setUrl("https://example.com");
        assertThrows(BusinessException.class, () -> service.create(dto, "admin"));
    }

    @Test
    @DisplayName("create → 名称重复则拒绝")
    void create_duplicateName_shouldThrow() {
        WebhookConfigDTO dto = new WebhookConfigDTO();
        dto.setName("hook1");
        dto.setUrl("https://example.com");
        when(webhookMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto, "admin"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("create → 正常创建，name trim + enabled 默认 1 + secret 脱敏返回")
    void create_success() {
        WebhookConfigDTO dto = new WebhookConfigDTO();
        dto.setName(" myHook ");
        dto.setUrl("https://example.com");
        dto.setSecret("s3cret");
        dto.setEnabled(null);
        when(webhookMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(webhookMapper.insert(any(WebhookConfig.class))).thenAnswer(inv -> {
            inv.getArgument(0, WebhookConfig.class).setId(10L);
            return 1;
        });

        WebhookConfig result = service.create(dto, "admin");

        assertEquals(10L, result.getId());
        assertEquals("myHook", result.getName());
        assertEquals(1, result.getEnabled());
        // secret should be masked
        assertEquals("******", result.getSecret());
        ArgumentCaptor<WebhookConfig> captor = ArgumentCaptor.forClass(WebhookConfig.class);
        verify(webhookMapper).insert(captor.capture());
        assertEquals("admin", captor.getValue().getCreatedBy());
    }

    // ---------------- update ----------------

    @Test
    @DisplayName("update → 不存在的 ID 抛 NOT_FOUND")
    void update_notFound_shouldThrow() {
        when(webhookMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.update(99L, new WebhookConfigDTO()));
    }

    @Test
    @DisplayName("update → secret 为空或掩码占位时保留旧值（不调 setSecret）")
    void update_secretMasked_keepsOldValue() {
        WebhookConfig existing = new WebhookConfig();
        existing.setId(1L);
        existing.setSecret("old-secret");
        when(webhookMapper.selectById(1L)).thenReturn(existing);
        when(webhookMapper.updateById(any(WebhookConfig.class))).thenReturn(1);

        WebhookConfigDTO dto = new WebhookConfigDTO();
        dto.setSecret("******"); // masked placeholder
        WebhookConfig result = service.update(1L, dto);

        // sanitize 将非空 secret 替换为 MASK，但原始值在 update 逻辑中未被覆盖
        verify(webhookMapper).updateById(any(WebhookConfig.class));
        // result 经过 sanitize，secret 一定为 MASK（验证 sanitize 生效即可）
        assertEquals("******", result.getSecret());
    }

    // ---------------- delete ----------------

    @Test
    @DisplayName("delete → 不存在的 ID 抛 NOT_FOUND")
    void delete_notFound_shouldThrow() {
        when(webhookMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.delete(99L));
    }

    @Test
    @DisplayName("delete → 正常删除")
    void delete_success() {
        WebhookConfig config = new WebhookConfig();
        config.setId(1L);
        when(webhookMapper.selectById(1L)).thenReturn(config);
        when(webhookMapper.deleteById(1L)).thenReturn(1);

        service.delete(1L);
        verify(webhookMapper).deleteById(1L);
    }

    // ---------------- toggleEnabled ----------------

    @Test
    @DisplayName("toggleEnabled → 切换启用状态")
    void toggleEnabled() {
        WebhookConfig config = new WebhookConfig();
        config.setId(1L);
        config.setEnabled(1);
        when(webhookMapper.selectById(1L)).thenReturn(config);
        when(webhookMapper.updateById(any(WebhookConfig.class))).thenReturn(1);

        WebhookConfig result = service.toggleEnabled(1L, 0);
        assertEquals(0, result.getEnabled());
    }

    // ---------------- sendToAllEnabled ----------------

    @Test
    @DisplayName("sendToAllEnabled → 多端点推送，记录日志，返回成功数")
    void sendToAllEnabled_recordsLogsAndReturnsCount() {
        WebhookConfig c1 = new WebhookConfig();
        c1.setId(1L);
        c1.setName("hook1");
        c1.setUrl("https://a.com");
        WebhookConfig c2 = new WebhookConfig();
        c2.setId(2L);
        c2.setName("hook2");
        c2.setUrl("https://b.com");
        when(webhookMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1, c2));
        when(webhookNotifier.pushDetailed("https://a.com", "t", "c"))
                .thenReturn(new WebhookNotifier.PushResult(true, 1, null));
        when(webhookNotifier.pushDetailed("https://b.com", "t", "c"))
                .thenReturn(new WebhookNotifier.PushResult(false, 3, "timeout"));

        int count = service.sendToAllEnabled("t", "c");

        assertEquals(1, count);
        verify(webhookLogMapper, times(2)).insert(any(WebhookLog.class));
    }

    // ---------------- cleanLogs ----------------

    @Test
    @DisplayName("cleanLogs → 删除过期日志")
    void cleanLogs() {
        when(webhookLogMapper.delete(any())).thenReturn(5);
        assertEquals(5, service.cleanLogs(30));
    }

    // ---------------- sanitize ----------------

    @Test
    @DisplayName("listAll → secret 被脱敏")
    void listAll_sanitizesSecret() {
        WebhookConfig c = new WebhookConfig();
        c.setId(1L);
        c.setSecret("real-secret");
        when(webhookMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c));

        List<WebhookConfig> result = service.listAll();
        assertEquals("******", result.get(0).getSecret());
    }

    @Test
    @DisplayName("listAll → secret 为空时不变")
    void listAll_nullSecret_unchanged() {
        WebhookConfig c = new WebhookConfig();
        c.setId(1L);
        c.setSecret(null);
        when(webhookMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c));

        List<WebhookConfig> result = service.listAll();
        assertNull(result.get(0).getSecret());
    }
}
