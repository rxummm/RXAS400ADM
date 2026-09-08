package com.rxas400adm.operation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("rx_operation")
public class Operation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String operationType;
    private String status;
    private String currentStep;
    private String targetType;
    private String targetName;
    private String requestData;
    private String resultData;
    private String errorCode;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetry;
    private String idempotencyKey;
    private String riskLevel;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
    private Integer version;
}