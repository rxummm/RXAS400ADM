package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统参数表（rx_config）：monitor.interval、
 * as400.login.groupRoleMapping 等。
 */
@Data
@TableName("rx_config")
public class SysConfig {

    @TableId
    private String configKey;

    private String configValue;

    private String description;
}