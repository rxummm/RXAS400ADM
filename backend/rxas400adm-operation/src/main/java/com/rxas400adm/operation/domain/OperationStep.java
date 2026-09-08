package com.rxas400adm.operation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("rx_operation_step")
public class OperationStep {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operationId;
    private String stepCode;
    private Integer stepOrder;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private String ibmiReturnCode;
    private String ibmiMessage;
    private String errorDetail;
    private Integer retryCount;

    /** CAS 乐观锁版本号 */
    @Version
    private Integer version;
}