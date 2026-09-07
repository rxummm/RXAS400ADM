package com.rxas400adm.as400.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * A8 系统值合规 VO
 */
@Data
public class SystemValueComplianceVO {

    private Long id;

    private Long serverId;

    private String serverName;

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
