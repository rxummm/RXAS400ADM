package com.rxas400adm.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知（rx_notification）：按用户名归属，ALERT/NOTICE/SYSTEM 等类型，
 * 顶栏铃铛 + 通知中心展示，WebSocket 实时推送新通知。
 */
@Data
@TableName("rx_notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** ALERT/NOTICE/SYSTEM/PERMISSION */
    private String type;

    private String title;

    private String content;

    /** 0=未读 1=已读 */
    private Integer readFlag;

    private LocalDateTime createdTime;
}