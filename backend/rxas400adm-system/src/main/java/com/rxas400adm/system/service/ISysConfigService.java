package com.rxas400adm.system.service;

import com.rxas400adm.system.dto.SysConfigDTO;
import com.rxas400adm.system.entity.SysConfig;

import java.util.List;

/**
 * 系统参数读写服务接口（rx_config）。
 */
public interface ISysConfigService {

    String get(String key, String defaultValue);

    void set(String key, String value, String description);

    /** 全部参数（敏感项值已掩码，管理端列表用） */
    List<SysConfig> list();

    /** 修改/新增参数（configKey 由路径传入；敏感项提交掩码占位则保留旧值） */
    SysConfig update(String configKey, SysConfigDTO dto);

    /** 删除参数 */
    void delete(String configKey);
}
