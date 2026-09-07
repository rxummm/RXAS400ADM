package com.rxas400adm.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.system.dto.SysConfigDTO;
import com.rxas400adm.system.entity.SysConfig;
import com.rxas400adm.system.mapper.SysConfigMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
class SysConfigServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysConfig.class);
    }

    @Mock
    private SysConfigMapper configMapper;

    private SysConfigService service;

    @BeforeEach
    void setUp() {
        service = new SysConfigService(configMapper);
    }

    @Test
    @DisplayName("get → 存在返回配置值")
    void get_exists_shouldReturnValue() {
        SysConfig config = new SysConfig();
        config.setConfigKey("smtp.host");
        config.setConfigValue("smtp.example.com");
        when(configMapper.selectById("smtp.host")).thenReturn(config);

        String result = service.get("smtp.host", "default");
        assertEquals("smtp.example.com", result);
    }

    @Test
    @DisplayName("get → 不存在返回 defaultValue")
    void get_notExists_shouldReturnDefault() {
        when(configMapper.selectById("missing")).thenReturn(null);

        String result = service.get("missing", "fallback");
        assertEquals("fallback", result);
    }

    @Test
    @DisplayName("get → configValue 为 null 返回 defaultValue")
    void get_nullValue_shouldReturnDefault() {
        SysConfig config = new SysConfig();
        config.setConfigKey("key");
        config.setConfigValue(null);
        when(configMapper.selectById("key")).thenReturn(config);

        String result = service.get("key", "default");
        assertEquals("default", result);
    }

    @Test
    @DisplayName("set → 新增或更新配置")
    void set_shouldUpsert() {
        service.set("key", "value", "描述");
        verify(configMapper).insertOrUpdate(any(SysConfig.class));
    }

    @Test
    @DisplayName("list → 返回全部配置（含掩码）")
    void list_shouldReturnWithMasking() {
        SysConfig normal = new SysConfig();
        normal.setConfigKey("smtp.host");
        normal.setConfigValue("smtp.example.com");
        SysConfig secret = new SysConfig();
        secret.setConfigKey("smtp.password");
        secret.setConfigValue("real-password");
        when(configMapper.selectList(null)).thenReturn(List.of(normal, secret));

        List<SysConfig> result = service.list();
        assertEquals(2, result.size());
        // smtp.password 应被掩码
        SysConfig maskedSecret = result.stream().filter(c -> "smtp.password".equals(c.getConfigKey())).findFirst().orElseThrow();
        assertNotEquals("real-password", maskedSecret.getConfigValue());
    }

    @Test
    @DisplayName("update → 掩码值保留旧密码")
    void update_maskedValue_shouldPreserveOld() {
        SysConfig existing = new SysConfig();
        existing.setConfigKey("smtp.password");
        existing.setConfigValue("old-pass");
        when(configMapper.selectById("smtp.password")).thenReturn(existing);

        SysConfigDTO dto = new SysConfigDTO();
        dto.setConfigValue("******");
        dto.setDescription("desc");

        service.update("smtp.password", dto);
        assertEquals("old-pass", existing.getConfigValue()); // 保留旧值
    }

    @Test
    @DisplayName("delete → 删除配置")
    void delete_shouldCallMapper() {
        SysConfig existing = new SysConfig();
        existing.setConfigKey("key");
        when(configMapper.selectById("key")).thenReturn(existing);
        service.delete("key");
        verify(configMapper).deleteById("key");
    }

    @Test
    @DisplayName("delete → 不存在抛 NOT_FOUND")
    void delete_notFound_shouldThrow() {
        when(configMapper.selectById("missing")).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.delete("missing"));
    }
}
