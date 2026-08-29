package com.rxas400adm.email.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件分组成员（rx_email_recipient）。
 */
@Data
@TableName("rx_email_recipient")
public class EmailRecipient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private String email;

    private Long userId;

    private Integer enabled;

    private LocalDateTime createdTime;
}
