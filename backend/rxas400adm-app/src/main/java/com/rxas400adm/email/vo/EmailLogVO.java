package com.rxas400adm.email.vo;

import com.rxas400adm.email.entity.EmailLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmailLogVO {

    private Long id;
    private String subject;
    private String recipients;
    private String channel;
    private String status;
    private String errorMessage;
    private String attachmentName;
    private LocalDateTime createdTime;

    public static EmailLogVO from(EmailLog entity) {
        EmailLogVO vo = new EmailLogVO();
        vo.setId(entity.getId());
        vo.setSubject(entity.getSubject());
        vo.setRecipients(entity.getRecipients());
        vo.setChannel(entity.getChannel());
        vo.setStatus(entity.getStatus());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setAttachmentName(entity.getAttachmentName());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
