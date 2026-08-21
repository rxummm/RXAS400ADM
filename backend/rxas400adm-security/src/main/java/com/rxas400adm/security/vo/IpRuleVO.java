package com.rxas400adm.security.vo;

import com.rxas400adm.security.entity.IpRule;

import java.time.LocalDateTime;

/**
 * IP 黑白名单视图（P2-10）：与 IpRule 字段一致。
 */
public record IpRuleVO(
        Long id,
        String ip,
        String type,
        String description,
        Integer enabled,
        String createdBy,
        LocalDateTime createdTime,
        LocalDateTime updatedTime) {

    public static IpRuleVO from(IpRule e) {
        return new IpRuleVO(
                e.getId(), e.getIp(), e.getType(), e.getDescription(), e.getEnabled(),
                e.getCreatedBy(), e.getCreatedTime(), e.getUpdatedTime());
    }
}
