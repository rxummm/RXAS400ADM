package com.rxas400adm.email.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件发送日志（rx_email_log）。
 */
@Data
@TableName("rx_email_log")
public class EmailLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String subject;

    private String recipients;

    private String channel;

    private String status;

    private String errorMessage;

    private String attachmentName;

    private LocalDateTime createdTime;
}
