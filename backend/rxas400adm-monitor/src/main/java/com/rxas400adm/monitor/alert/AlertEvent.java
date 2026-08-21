package com.rxas400adm.monitor.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警事件表（rx_alert_event）：状态 OPEN / ACK / CLOSED
 */
@Data
@TableName("rx_alert_event")
public class AlertEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;

    private Long ruleId;

    private String level;

    private String message;

    private String status;

    private LocalDateTime createdTime;
}
