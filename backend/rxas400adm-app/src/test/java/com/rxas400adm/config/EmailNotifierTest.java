package com.rxas400adm.config;

import com.rxas400adm.system.service.ISysConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotifierTest {

    @Mock
    private ISysConfigService sysConfigService;

    @Test
    void sendAlert_noHost_shouldSkip() {
        when(sysConfigService.get("alert.email.host", "")).thenReturn("");
        EmailNotifier notifier = new EmailNotifier(sysConfigService);
        assertDoesNotThrow(() -> notifier.sendAlert("告警标题", "告警内容"));
    }

    @Test
    void sendAlert_hostButNoRecipient_shouldWarnAndSkip() {
        when(sysConfigService.get("alert.email.host", "")).thenReturn("smtp.example.com");
        when(sysConfigService.get("alert.email.to", "")).thenReturn("");
        EmailNotifier notifier = new EmailNotifier(sysConfigService);
        assertDoesNotThrow(() -> notifier.sendAlert("告警标题", "告警内容"));
    }
}
