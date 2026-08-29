package com.rxas400adm.config;

import com.rxas400adm.email.entity.EmailLog;
import com.rxas400adm.email.mapper.EmailConfigMapper;
import com.rxas400adm.email.mapper.EmailLogMapper;
import com.rxas400adm.email.service.EmailService;
import com.rxas400adm.system.service.ISysConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailConfigMapper emailConfigMapper;
    @Mock
    private EmailLogMapper emailLogMapper;
    @Mock
    private ISysConfigService sysConfigService;

    @Test
    void send_noRecipient_shouldSkip() {
        EmailService service = new EmailService(emailConfigMapper, emailLogMapper, sysConfigService);
        // alert() 默认 recipients=null → send() 提前返回，无需任何 stub
        assertDoesNotThrow(() -> service.send(
                com.rxas400adm.email.MailMessage.alert("告警标题", "告警内容")));
        verify(emailLogMapper, never()).insert(any(EmailLog.class));
    }

    @Test
    void send_noHost_shouldSkip() {
        EmailService service = new EmailService(emailConfigMapper, emailLogMapper, sysConfigService);
        assertDoesNotThrow(() -> service.send(
                com.rxas400adm.email.MailMessage.alert("告警标题", "告警内容")));
    }
}
