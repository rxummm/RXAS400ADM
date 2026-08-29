package com.rxas400adm.email.vo;

import com.rxas400adm.email.entity.EmailRecipientGroup;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmailGroupVO {

    private Long id;
    private String groupName;
    private String description;
    private Integer memberCount;
    private LocalDateTime createdTime;

    public static EmailGroupVO from(EmailRecipientGroup entity, int memberCount) {
        EmailGroupVO vo = new EmailGroupVO();
        vo.setId(entity.getId());
        vo.setGroupName(entity.getGroupName());
        vo.setDescription(entity.getDescription());
        vo.setMemberCount(memberCount);
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
