package com.rxas400adm.email.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件配置表（rx_email_config）：SMTP 配置键值对。
 */
@Data
@TableName("rx_email_config")
public class EmailConfig {

    @TableId(type = IdType.INPUT)  // configKey 为 String 类型，手动赋值
    private String configKey;

    private String configValue;

    private String description;

    private LocalDateTime updatedTime;
}