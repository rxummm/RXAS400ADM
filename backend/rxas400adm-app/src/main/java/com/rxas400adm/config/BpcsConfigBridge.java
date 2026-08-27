package com.rxas400adm.config;

import com.rxas400adm.as400.service.BpcsOrderServiceImpl;
import com.rxas400adm.system.service.SysConfigService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 【第六章·P1】BPCS 库名配置读取桥接：
 * as400 模块不依赖 system，故以 BpcsOrderServiceImpl.SysConfigServiceHolder 函数式接口
 * 在 app 装配层接入 SysConfigService（rx_config 键 bpcs.library，系统配置页可维护）。
 */
@Configuration
public class BpcsConfigBridge {

    @Bean
    public BpcsOrderServiceImpl.SysConfigServiceHolder bpcsLibraryConfigHolder(
            SysConfigService sysConfigService) {
        return sysConfigService::get;
    }
}
