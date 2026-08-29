package com.rxas400adm.email.vo;

import com.rxas400adm.email.entity.EmailConfig;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmailConfigVO {

    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updatedTime;

    public static EmailConfigVO from(EmailConfig entity) {
        EmailConfigVO vo = new EmailConfigVO();
        vo.setConfigKey(entity.getConfigKey());
        vo.setConfigValue(entity.getConfigValue());
        vo.setDescription(entity.getDescription());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }
}
