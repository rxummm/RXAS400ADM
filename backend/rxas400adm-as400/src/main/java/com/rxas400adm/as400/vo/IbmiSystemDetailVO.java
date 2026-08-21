package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.IbmiSystem;

import java.time.LocalDateTime;

/**
 * IBM i 实例管理视图（AS400_MANAGE）：与 IbmiSystem 字段契约解耦。
 * 供资产清单管理页 CRUD 消费——含 username 连接凭据，不含 passwordEncrypt。
 */
public record IbmiSystemDetailVO(
        Long id,
        String name,
        String host,
        Integer port,
        String username,
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

    public static IbmiSystemDetailVO from(IbmiSystem e) {
        return new IbmiSystemDetailVO(
                e.getId(), e.getName(), e.getHost(), e.getPort(), e.getUsername(),
                e.getEnvironment(), e.getRegion(), e.getCriticalLevel(), e.getHaGroup(),
                e.getSslEnabled(), e.getDefaultLibraries(), e.getCcsid(), e.getEnabled(),
                e.getDefaultServer(), e.getStatus(), e.getDescription(), e.getSortOrder(),
                e.getCreatedTime(), e.getConnectionStatus());
    }
}