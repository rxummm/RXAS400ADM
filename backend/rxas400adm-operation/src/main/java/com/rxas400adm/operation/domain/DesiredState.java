package com.rxas400adm.operation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("rx_desired_state")
public class DesiredState {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String targetType;
    private String targetName;
    private String stateData;
    private Integer version;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}