package com.rxas400adm.as400.collaboration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rx_collaboration_notification")
public class CollaborationNotification {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long collaborationId;
    private String sender;
    private String recipient;
    private String message;
    /** SYSTEM / EMAIL / SMS */
    private String channel;
    private Boolean isRead;
    private LocalDateTime createdTime;
}
