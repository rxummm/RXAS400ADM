package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * IBM i 服务器写请求 DTO（create/update 共用）。
 * 不含 id/status/connectionStatus/createdTime 等服务端托管字段，防伪造。
 * passwordEncrypt 为明文口令（与服务端字段同名，更新时为空/掩码则保留旧值）。
 */
@Data
public class IbmiSystemDTO {

    @NotBlank(message = "{validation.notBlank}")
    private String name;

    @NotBlank(message = "{validation.notBlank}")
    private String host;

    @NotNull(message = "{validation.notNull}")
    private Integer port;

    @NotBlank(message = "{validation.notBlank}")
    private String username;

    /** 明文口令：创建必填；更新时为空或掩码占位则保留旧值 */
    private String passwordEncrypt;

    private String environment;

    private String region;

    private String criticalLevel;

    private String haGroup;

    private Boolean sslEnabled;

    private String defaultLibraries;

    private Integer ccsid;

    private Boolean enabled;

    private Boolean defaultServer;

    private String description;

    private Integer sortOrder;
}
