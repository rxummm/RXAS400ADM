package com.rxas400adm.security.dto;

import com.rxas400adm.security.entity.IpRule;
import jakarta.validation.constraints.Pattern;

/**
 * IP 黑白名单更新请求（P2-10 遗留）：全字段可选（null = 不修改，
 * 与 IpRuleService.update 的局部更新语义一致）；id 仅取自路径。
 */
public record IpRuleUpdateDTO(
        String ip,
        @Pattern(regexp = "BLACK|WHITE", message = "{validation.pattern.ipType}")
        String type,
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
