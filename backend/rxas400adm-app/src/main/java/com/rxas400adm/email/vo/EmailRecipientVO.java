package com.rxas400adm.email.vo;

import com.rxas400adm.email.entity.EmailRecipient;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件分组成员 VO
 */
@Data
public class EmailRecipientVO {

    private Long id;
    private Long groupId;
    private String email;
    private Long userId;
    private Integer enabled;
    private LocalDateTime createdTime;

    public static EmailRecipientVO from(EmailRecipient entity) {
        EmailRecipientVO vo = new EmailRecipientVO();
        vo.setId(entity.getId());
        vo.setGroupId(entity.getGroupId());
        vo.setEmail(entity.getEmail());
        vo.setUserId(entity.getUserId());
        vo.setEnabled(entity.getEnabled());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}