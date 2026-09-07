package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IBM i 系统值合规检查（rx_system_value_compliance）
 */
@Data
@TableName("rx_system_value_compliance")
public class SystemValueCompliance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long serverId;

    private String systemValue;

    private String currentValue;

    private String expectedValue;

    private String complianceStatus;

    private String severity;

    private String description;

    private String remediation;

    private LocalDateTime lastChecked;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
