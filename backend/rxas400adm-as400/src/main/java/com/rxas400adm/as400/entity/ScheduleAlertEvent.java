package com.rxas400adm.as400.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调度任务失败告警事件（映射 rx_alert_event，复用监控告警展示通道）。\n * instanceId 存调度任务的目标服务器 ID，供前端按服务器关联。\n */
@Data
@TableName("rx_alert_event")
public class ScheduleAlertEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;

    private Long ruleId;

    private String level;

    private String message;

    private String status;

    /** E1 升级通知去重：0=未发送 1=已发送（与 AlertEvent 保持一致） */
    private Integer upgradeNotified;

    private LocalDateTime createdTime;
}
