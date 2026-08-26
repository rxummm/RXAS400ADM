package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.system.entity.AuditLog;
import com.rxas400adm.system.mapper.AuditLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogMapper auditLogMapper;

    private AuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(auditLogMapper);
    }

    /** 类型安全占位符：利用 any() 的目标类型推断消除裸 Class 字面量的 unchecked 转换警告 */
    private static LambdaQueryWrapper<AuditLog> anyWrapper() {
        return any();
    }

    // ---------------- page ----------------

    @Test
    @DisplayName("page → 无过滤条件时返回全部")
    void page_noFilters() {
        when(auditLogMapper.selectPage(any(), anyWrapper()))
                .thenReturn(new Page<>());
        var result = service.page(1, 10, null, null, null, null);
        assertNotNull(result);
        verify(auditLogMapper).selectPage(any(), anyWrapper());
    }

    // ---------------- auditLogin ----------------

    @Test
    @DisplayName("auditLogin → 正常写入审计日志")
    void auditLogin_success() {
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        service.auditLogin(new LoginAuditContext(
                "LOGIN_SUCCESS", "admin", "127.0.0.1", "PLATFORM", null, "登录成功"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals("admin", log.getUserName());
        assertEquals("LOGIN_SUCCESS", log.getAction());
        assertEquals("登录安全", log.getModule());
        assertEquals("127.0.0.1", log.getIp());
        assertEquals("PLATFORM", log.getTarget());
        assertNotNull(log.getCreatedTime());
    }

    @Test
    @DisplayName("auditLogin → 写入异常时吞异常不影响主流程")
    void auditLogin_exceptionSwallowed() {
        when(auditLogMapper.insert(any(AuditLog.class))).thenThrow(new RuntimeException("DB down"));

        assertDoesNotThrow(() -> service.auditLogin(new LoginAuditContext(
                "LOGIN_FAIL", "alice", "10.0.0.1", "AS400", 1L, "密码错误")));
    }

    @Test
    @DisplayName("auditLogin → serverId 拼接到 target 中")
    void auditLogin_withServerId() {
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        service.auditLogin(new LoginAuditContext(
                "LOGIN_SUCCESS", "bob", "192.168.1.1", "AS400", 42L, "ok"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertEquals("AS400 server=42", captor.getValue().getTarget());
    }
}
