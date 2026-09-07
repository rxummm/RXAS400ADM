package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IBM i 备份状态记录（rx_backup_status）
 */
@Data
@TableName("rx_backup_status")
public class BackupStatus {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long serverId;

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
