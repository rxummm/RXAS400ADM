package com.rxas400adm.security.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.security.config.LoginSecurityProperties;
import com.rxas400adm.security.entity.LoginAttempt;
import com.rxas400adm.security.mapper.LoginAttemptMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LoginAttemptServiceTest {

    @Mock
    private LoginAttemptMapper attemptMapper;

    /** 【P2】运行时阈值来源 mock：get(key, default) 统一回传 default，等价于 yml 缺省 */
    @Mock
    private com.rxas400adm.system.service.SysConfigService sysConfigService;

    private LoginAttemptService service;

    /** 模拟数据库行：selectOne 返回，incrementFailure 原子累加 */
    private LoginAttempt dbRow;

    @BeforeEach
    void setUp() {
        // R7：构造新增 LoginSecurityProperties——默认值 5/15/20 与原 @Value 默认逐字一致，断言不变
        // P2：追加 SysConfigService——stub get(key, default) 原样回传 default（等价未配置 rx_config）
        service = new LoginAttemptService(new LoginSecurityProperties(), sysConfigService, attemptMapper);
        lenient().when(sysConfigService.get(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        dbRow = new LoginAttempt();
        dbRow.setUsername("admin");
        dbRow.setServerId(0L);
        dbRow.setFailedCount(0);
        org.mockito.Mockito.lenient().when(attemptMapper.selectOne(any(Wrapper.class))).thenAnswer(inv -> dbRow);
        org.mockito.Mockito.lenient().when(attemptMapper.incrementFailure(eq("admin"), eq(0L), any())).thenAnswer(inv -> {
            dbRow.setFailedCount(dbRow.getFailedCount() + 1);
            dbRow.setLastFailTime(LocalDateTime.now());
            return 1;
        });
    }

    @Test
    void checkUsernameLock_lockedInFuture_shouldThrow() {
        dbRow.setFailedCount(5);
        dbRow.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        assertThrows(BusinessException.class, () -> service.checkUsernameLock("admin", null));
    }

    @Test
    void checkUsernameLock_expiredLock_shouldClear() {
        dbRow.setFailedCount(5);
        dbRow.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        service.checkUsernameLock("admin", null);
        verify(attemptMapper).delete(any(Wrapper.class));
    }

    @Test
    void registerFailure_firstTime_shouldInsert() {
        when(attemptMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        service.registerFailure("newuser", null, "10.0.0.1");
        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(attemptMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getFailedCount());
        assertEquals(0L, captor.getValue().getServerId());
    }

    @Test
    void registerFailure_fifthFailure_shouldLock() {
        dbRow.setFailedCount(4);
        service.registerFailure("admin", null, "10.0.0.1");
        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(attemptMapper).update(captor.capture(), any(Wrapper.class));
        assertNotNull(captor.getValue().getLockedUntil());
    }

    @Test
    void clearFailure_shouldDeleteRow() {
        service.clearFailure("admin", null);
        verify(attemptMapper).delete(any(Wrapper.class));
    }

    @Test
    void registerFailure_belowThreshold_shouldNotLock() {
        dbRow.setFailedCount(2);
        service.registerFailure("admin", null, "10.0.0.1");
        verify(attemptMapper, never()).update(any(LoginAttempt.class), any(Wrapper.class));
    }

    @Test
    void listAttempts_withServerId_shouldFilter() {
        service.listAttempts(1L);
        verify(attemptMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void checkIpRate_underLimit_shouldPass() {
        // P3-4：上限改为可配置实例字段（默认 20），测试用字面量
        for (int i = 0; i < 20; i++) {
            service.checkIpRate("10.0.0.2");
        }
        assertThrows(BusinessException.class, () -> service.checkIpRate("10.0.0.2"));
        assertDoesNotThrow(() -> service.checkIpRate("10.0.0.3"));
    }
}