package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.Notification;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知视图对象（B5：禁止直接返回 Entity）。
 */
@Data
public class NotificationVO {

    private Long id;
    private String username;
    private String type;
    private String title;
    private String content;
    private Integer readFlag;
    private LocalDateTime createdTime;

    public static NotificationVO from(Notification entity) {
        NotificationVO vo = new NotificationVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setType(entity.getType());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setReadFlag(entity.getReadFlag());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
