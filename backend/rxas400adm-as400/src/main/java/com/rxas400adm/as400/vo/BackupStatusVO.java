package com.rxas400adm.as400.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * A4 备份状态 VO
 */
@Data
public class BackupStatusVO {

    private Long id;

    private Long serverId;

    private String serverName;

    private String backupName;

    private String backupType;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationSeconds;

    private Integer objectsCount;

    private Long sizeBytes;

    private String mediaName;

    private String errorMessage;

    private LocalDateTime createdTime;
}
