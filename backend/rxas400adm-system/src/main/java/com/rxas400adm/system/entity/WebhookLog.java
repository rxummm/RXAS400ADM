package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook 发送日志（rx_webhook_log）：每次推送一条记录，追溯发送成败。
 */
@Data
@TableName("rx_webhook_log")
public class WebhookLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long webhookId;

    /** 触发时名称（配置删除后仍可追溯） */
    private String webhookName;

    private String title;

    private String message;

    /** 1=成功 0=失败 */
    private Integer success;

    private Integer attempts;

    private String errorMsg;

    private LocalDateTime createdTime;
}