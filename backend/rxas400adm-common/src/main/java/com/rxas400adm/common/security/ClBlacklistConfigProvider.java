package com.rxas400adm.common.security;

import java.util.List;

/**
 * 【第六章·P1】CL 黑名单运行时配置提供者（SPI）。
 *
 * <p>common 模块不能反向依赖 system 的 SysConfigService，
 * 故以接口解耦：system 模块提供 rx_config 实现（{@code cl.blacklist.*} 两键），
 * 未配置/读取失败时由实现方负责降级为安全默认值（enabled=true、扩展动词为空）。
 */
public interface ClBlacklistConfigProvider {

    /** 黑名单总开关（rx_config: cl.blacklist.enabled，缺省 true） */
    boolean enabled();

    /** 追加拦截动词（rx_config: cl.blacklist.extra-verbs，逗号分隔，已大写去空白） */
    List<String> extraVerbs();
}
