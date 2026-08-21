package com.rxas400adm.system.service;

import com.rxas400adm.common.security.SecretMasker;
import com.rxas400adm.system.dto.SysConfigDTO;
import com.rxas400adm.system.entity.SysConfig;
import com.rxas400adm.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统参数读写（rx_config）。掩码逻辑（P2-6）收敛于此（R1 分层清零）。
 */
@Service
@RequiredArgsConstructor
public class SysConfigService implements ISysConfigService {

    private final SysConfigMapper configMapper;

    public String get(String key, String defaultValue) {
        SysConfig config = configMapper.selectById(key);
        return config == null || config.getConfigValue() == null ? defaultValue : config.getConfigValue();
    }

    public void set(String key, String value, String description) {
        SysConfig config = new SysConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription(description);
        configMapper.insertOrUpdate(config);
    }

    @Override
    public List<SysConfig> list() {
        List<SysConfig> configs = configMapper.selectList(null);
        // P2-6：敏感配置（SMTP 密码等）不回显明文，统一掩码
        configs.forEach(c -> {
            if (c.getConfigValue() != null && SecretMasker.isSecretKey(c.getConfigKey())) {
                c.setConfigValue(SecretMasker.MASK);
            }
        });
        return configs;
    }

    @Override
    public SysConfig update(String configKey, SysConfigDTO dto) {
        SysConfig config = configMapper.selectById(configKey);
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(configKey);
        }
        // P2-6：敏感项提交掩码占位（前端未改动）→ 保留旧值
        String newValue = dto.getConfigValue();
        if (SecretMasker.isSecretKey(configKey) && SecretMasker.isMasked(newValue)) {
            newValue = config.getConfigValue();
        }
        config.setConfigValue(newValue);
        config.setDescription(dto.getDescription());
        configMapper.insertOrUpdate(config);
        return config;
    }

    @Override
    public void delete(String configKey) {
        configMapper.deleteById(configKey);
    }
}
