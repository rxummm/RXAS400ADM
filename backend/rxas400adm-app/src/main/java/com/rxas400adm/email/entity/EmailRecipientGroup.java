package com.rxas400adm.email.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件收件人分组（rx_email_recipient_group）。
 */
@Data
@TableName("rx_email_recipient_group")
public class EmailRecipientGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupName;

    private String description;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
