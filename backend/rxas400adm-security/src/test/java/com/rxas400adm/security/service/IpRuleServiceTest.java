package com.rxas400adm.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.security.dto.IpRuleCreateDTO;
import com.rxas400adm.security.entity.IpRule;
import com.rxas400adm.security.mapper.IpRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IpRuleService 测试")
class IpRuleServiceTest {

    @Mock
    private IpRuleMapper ipRuleMapper;

    private IpRuleService service;

    @BeforeEach
    void setUp() {
        service = new IpRuleService(ipRuleMapper);
    }

    // ========== checkIp ==========

    @Test
    @DisplayName("checkIp() — null/blank IP 不拦截")
    void checkIp_nullOrBlank_passes() {
        assertDoesNotThrow(() -> service.checkIp(null));
        assertDoesNotThrow(() -> service.checkIp(""));
        assertDoesNotThrow(() -> service.checkIp("  "));
        verify(ipRuleMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("checkIp() — 无启用规则不拦截")
    void checkIp_noRules_passes() {
        when(ipRuleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertDoesNotThrow(() -> service.checkIp("192.168.1.1"));
    }

    @Test
    @DisplayName("checkIp() — 黑名单命中拒绝")
    void checkIp_blackHit_throws() {
        IpRule black = new IpRule();
        black.setType("BLACK");
        black.setIp("192.168.1.100");
        black.setEnabled(1);
        when(ipRuleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(black));

        assertThrows(BusinessException.class, () -> service.checkIp("192.168.1.100"));
    }

    @Test
    @DisplayName("checkIp() — 白名单未命中拒绝")
    void checkIp_whiteMiss_throws() {
        IpRule white = new IpRule();
        white.setType("WHITE");
        white.setIp("10.0.0.1");
        white.setEnabled(1);
        when(ipRuleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(white));

        assertThrows(BusinessException.class, () -> service.checkIp("192.168.1.1"));
    }

    @Test
    @DisplayName("checkIp() — 白名单命中放行")
    void checkIp_whiteHit_passes() {
        IpRule white = new IpRule();
        white.setType("WHITE");
        white.setIp("192.168.1.*");
        white.setEnabled(1);
        when(ipRuleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(white));

        assertDoesNotThrow(() -> service.checkIp("192.168.1.50"));
    }

    // ========== match ==========

    @Test
    @DisplayName("match() — 精确匹配")
    void match_exact() {
        assertTrue(service.match("192.168.1.1", "192.168.1.1"));
        assertFalse(service.match("192.168.1.1", "192.168.1.2"));
    }

    @Test
    @DisplayName("match() — 通配符匹配")
    void match_wildcard() {
        assertTrue(service.match("192.168.1.*", "192.168.1.100"));
        assertFalse(service.match("192.168.1.*", "192.168.2.1"));
    }

    @Test
    @DisplayName("match() — CIDR 匹配")
    void match_cidr() {
        assertTrue(service.match("10.0.0.0/8", "10.1.2.3"));
        assertFalse(service.match("10.0.0.0/8", "192.168.1.1"));
    }

    // ========== CRUD ==========

    @Test
    @DisplayName("create() — 正常创建")
    void create_normal() {
        IpRuleCreateDTO dto = new IpRuleCreateDTO("192.168.1.1", "BLACK", "测试", 1);
        when(ipRuleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(ipRuleMapper.insert(any(IpRule.class))).thenReturn(1);

        IpRule result = service.create(dto, "admin");

        assertEquals("192.168.1.1", result.getIp());
        assertEquals("BLACK", result.getType());
        assertEquals("admin", result.getCreatedBy());
    }

    @Test
    @DisplayName("create() — 重复 IP 抛异常")
    void create_duplicate_throws() {
        IpRuleCreateDTO dto = new IpRuleCreateDTO("192.168.1.1", "BLACK", null, 1);
        when(ipRuleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.create(dto, "admin"));
    }

    @Test
    @DisplayName("create() — 无效类型抛异常")
    void create_invalidType_throws() {
        IpRuleCreateDTO dto = new IpRuleCreateDTO("192.168.1.1", "INVALID", null, 1);
        assertThrows(BusinessException.class, () -> service.create(dto, "admin"));
    }

    @Test
    @DisplayName("delete() — 正常删除")
    void delete_normal() {
        IpRule rule = new IpRule();
        rule.setId(1L);
        when(ipRuleMapper.selectById(1L)).thenReturn(rule);
        when(ipRuleMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> service.delete(1L));
        verify(ipRuleMapper).deleteById(1L);
    }

    @Test
    @DisplayName("delete() — 不存在抛异常")
    void delete_notExists_throws() {
        when(ipRuleMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.delete(99L));
    }
}