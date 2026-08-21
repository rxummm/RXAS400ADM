package com.rxas400adm.security.dto;

import com.rxas400adm.security.entity.IpRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * IP 黑白名单新增请求（P2-10 遗留）：仅暴露可写字段，
 * 服务端字段（createdBy/createdTime/updatedTime/id）不可由请求体注入。
 */
public record IpRuleCreateDTO(
        @NotBlank(message = "{validation.notBlank}") String ip,
        @NotBlank(message = "{validation.notBlank}") @Pattern(regexp = "BLACK|WHITE", message = "{validation.pattern.ipType}") String type,
        String description,
        Integer enabled) {

    public IpRule toEntity() {
        IpRule rule = new IpRule();
        rule.setIp(ip);
        rule.setType(type);
        rule.setDescription(description);
        rule.setEnabled(enabled);
        return rule;
    }
}
