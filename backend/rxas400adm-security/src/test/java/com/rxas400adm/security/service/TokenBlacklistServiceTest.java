package com.rxas400adm.security.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.security.config.JwtProperties;
import com.rxas400adm.security.entity.TokenBlacklist;
import com.rxas400adm.security.mapper.TokenBlacklistMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
class TokenBlacklistServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, TokenBlacklist.class);
    }

    @Mock
    private TokenBlacklistMapper blacklistMapper;

    private JwtProperties jwtProperties;
    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setBlacklistEnabled(true);
        jwtProperties.setBlacklistFailClosed(true);
        service = new TokenBlacklistService(blacklistMapper, jwtProperties);
    }

    @Test
    @DisplayName("isBlacklisted → 黑名单关闭时返回 false")
    void isBlacklisted_disabled_shouldReturnFalse() {
        jwtProperties.setBlacklistEnabled(false);
        assertFalse(service.isBlacklisted("jti-1"));
        verify(blacklistMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("isBlacklisted → null/空 jti 返回 false")
    void isBlacklisted_nullJti_shouldReturnFalse() {
        assertFalse(service.isBlacklisted(null));
        assertFalse(service.isBlacklisted(""));
        assertFalse(service.isBlacklisted("  "));
    }

    @Test
    @DisplayName("isBlacklisted → 已吊销返回 true")
    void isBlacklisted_blacklisted_shouldReturnTrue() {
        when(blacklistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertTrue(service.isBlacklisted("jti-1"));
    }

    @Test
    @DisplayName("isBlacklisted → 未吊销返回 false")
    void isBlacklisted_notBlacklisted_shouldReturnFalse() {
        when(blacklistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertFalse(service.isBlacklisted("jti-1"));
    }

    @Test
    @DisplayName("isBlacklisted → DB 异常 + fail-closed 返回 true")
    void isBlacklisted_dbError_failClosed_shouldReturnTrue() {
        when(blacklistMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("DB down"));
        assertTrue(service.isBlacklisted("jti-1"));
    }

    @Test
    @DisplayName("isBlacklisted → DB 异常 + fail-open 返回 false")
    void isBlacklisted_dbError_failOpen_shouldReturnFalse() {
        jwtProperties.setBlacklistFailClosed(false);
        when(blacklistMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("DB down"));
        assertFalse(service.isBlacklisted("jti-1"));
    }

    @Test
    @DisplayName("blacklist → 有效 jti 登记吊销")
    void blacklist_valid_shouldInsert() {
        when(blacklistMapper.insert(any(TokenBlacklist.class))).thenReturn(1);
        service.blacklist("jti-1", "admin", 3600000L);
        verify(blacklistMapper).insert(any(TokenBlacklist.class));
    }

    @Test
    @DisplayName("blacklist → ttl<=0 忽略")
    void blacklist_zeroTtl_shouldIgnore() {
        service.blacklist("jti-1", "admin", 0);
        verify(blacklistMapper, never()).insert(any(TokenBlacklist.class));
    }

    @Test
    @DisplayName("blacklist → null jti 忽略")
    void blacklist_nullJti_shouldIgnore() {
        service.blacklist(null, "admin", 3600000L);
        verify(blacklistMapper, never()).insert(any(TokenBlacklist.class));
    }

    @Test
    @DisplayName("cleanup → 删除过期记录")
    void cleanup_shouldDeleteExpired() {
        when(blacklistMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(5);
        service.cleanup();
        verify(blacklistMapper).delete(any(LambdaQueryWrapper.class));
    }
}
