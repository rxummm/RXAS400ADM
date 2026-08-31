package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IBM i 实例注册表（rx_ibmi_system），支持多 AS400 环境管理。
 * 环境级别：PROD / TEST / DEV / DR
 */
@Data
@TableName("rx_ibmi_system")
public class IbmiSystem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("system_name")
    private String name;

    private String host;

    private Integer port;

    private String username;

    @JsonIgnore
    private String passwordEncrypt;

    /** PROD / TEST / DEV / DR */
    private String environment;

    private String region;

    private String criticalLevel;

    private String haGroup;

    private Boolean sslEnabled;

    /** 默认库列表，逗号分隔 */
    private String defaultLibraries;

    private Integer ccsid;

    private Boolean enabled;

    /** 是否默认服务器（前端选择器未选择时的兜底） */
    private Boolean defaultServer;

    private String status;

    private String description;

    private Integer sortOrder;

    private LocalDateTime createdTime;

    @TableField(exist = false)
    private String connectionStatus;
}
