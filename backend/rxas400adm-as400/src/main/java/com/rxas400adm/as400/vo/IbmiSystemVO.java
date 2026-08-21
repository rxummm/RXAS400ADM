package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.IbmiSystem;

import java.time.LocalDateTime;

/**
 * IBM i 实例视图（N2 加固）：与 IbmiSystem 字段契约解耦。
 * 供全局服务器选择器 / Dashboard / 报表 / 定时任务 / 告警规则 / 巡检 / 对比等
 * 普通已登录用户消费——只保留运维展示所需字段，剔除连接凭据：
 * username / passwordEncrypt 不返回（资产清单管理页经 AS400_MANAGE 的 detail 接口获取）。
 */
public record IbmiSystemVO(
        Long id,
        String name,
        String host,
        Integer port,
        String environment,
        String region,
        String criticalLevel,
        String haGroup,
        Boolean sslEnabled,
        String defaultLibraries,
        Integer ccsid,
        Boolean enabled,
        Boolean defaultServer,
        String status,
        String description,
        Integer sortOrder,
        LocalDateTime createdTime,
        String connectionStatus) {

    public static IbmiSystemVO from(IbmiSystem e) {
        return new IbmiSystemVO(
                e.getId(), e.getName(), e.getHost(), e.getPort(), e.getEnvironment(),
                e.getRegion(), e.getCriticalLevel(), e.getHaGroup(), e.getSslEnabled(),
                e.getDefaultLibraries(), e.getCcsid(), e.getEnabled(), e.getDefaultServer(),
                e.getStatus(), e.getDescription(), e.getSortOrder(), e.getCreatedTime(),
                e.getConnectionStatus());
    }
}