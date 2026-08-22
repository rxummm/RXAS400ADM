package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.AuditLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志视图对象（B5：禁止直接返回 Entity）。
 */
@Data
public class AuditLogVO {

    private Long id;
    private String userName;
    private String action;
    private String module;
    private String target;
    private String ip;
    private String detail;
    private LocalDateTime createdTime;

    public static AuditLogVO from(AuditLog entity) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(entity.getId());
        vo.setUserName(entity.getUserName());
        vo.setAction(entity.getAction());
        vo.setModule(entity.getModule());
        vo.setTarget(entity.getTarget());
        vo.setIp(entity.getIp());
        vo.setDetail(entity.getDetail());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
