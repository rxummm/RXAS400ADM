package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "服务器名称", example = "PROD-SYS01")
    private String name;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "主机地址", example = "192.168.1.100")
    private String host;

    @NotNull(message = "{validation.notNull}")
    @Schema(description = "端口", example = "23")
    private Integer port;

    @NotBlank(message = "{validation.notBlank}")
    @Schema(description = "用户名", example = "QSECOFR")
    private String username;

    @Schema(description = "明文口令：创建必填，更新时为空/掩码则保留旧值")
    private String passwordEncrypt;

    @Schema(description = "环境", example = "PROD")
    private String environment;

    @Schema(description = "区域", example = "CN-SH")
    private String region;

    @Schema(description = "关键级别", example = "HIGH")
    private String criticalLevel;

    @Schema(description = "HA组", example = "GROUP-01")
    private String haGroup;

    @Schema(description = "是否启用SSL", example = "true")
    private Boolean sslEnabled;

    @Schema(description = "默认库列表", example = "QGPL,QTEMP")
    private String defaultLibraries;

    @Schema(description = "CCSID", example = "1386")
    private Integer ccsid;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "默认服务器", example = "true")
    private Boolean defaultServer;

    @Schema(description = "描述", example = "生产环境服务器")
    private String description;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;
}