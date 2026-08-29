package com.rxas400adm.security.dto;

import com.rxas400adm.security.entity.IpRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/**
 * IP 黑白名单更新请求（P2-10 遗留）：全字段可选（null = 不修改，
 * 与 IpRuleService.update 的局部更新语义一致）；id 仅取自路径。
 */
public record IpRuleUpdateDTO(
        @Schema(description = "IP地址", example = "192.168.1.100") String ip,
        @Pattern(regexp = "BLACK|WHITE", message = "{validation.pattern.ipType}")
        @Schema(description = "类型：BLACK/WHITE", example = "WHITE") String type,
        @Schema(description = "描述", example = "办公网络") String description,
        @Schema(description = "是否启用", example = "1") Integer enabled) {

    public IpRule toEntity() {
        IpRule rule = new IpRule();
        rule.setIp(ip);
        rule.setType(type);
        rule.setDescription(description);
        rule.setEnabled(enabled);
        return rule;
    }
}