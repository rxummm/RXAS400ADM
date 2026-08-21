package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.SysConfig;

/**
 * 系统参数视图（P2-10 模式）：与 SysConfig 字段一致，隔离 Controller 直返 Entity。
 */
public record SysConfigVO(
        String configKey,
        String configValue,
        String description) {

    public static SysConfigVO from(SysConfig e) {
        return new SysConfigVO(e.getConfigKey(), e.getConfigValue(), e.getDescription());
    }
}
