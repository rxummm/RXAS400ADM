package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook 推送配置（rx_webhook）：告警/通知对外推送的多个端点。
 */
@Data
@TableName("rx_webhook")
public class WebhookConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String url;

    private String secret;

    /** 1=启用 0=停用 */
    private Integer enabled;

    private String description;

    private String createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}