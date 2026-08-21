package com.rxas400adm.system.vo;

import com.rxas400adm.system.entity.PermissionRequest;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PermissionRequestVO {

    private Long id;
    private String username;
    private String permissionCode;
    private String menuIds;
    private String menuNames;
    private String reason;
    private String status;
    private String approver;
    private String approveComment;
    private LocalDateTime createdTime;

    public static PermissionRequestVO from(PermissionRequest entity) {
        PermissionRequestVO vo = new PermissionRequestVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setPermissionCode(entity.getPermissionCode());
        vo.setMenuIds(entity.getMenuIds());
        vo.setMenuNames(entity.getMenuNames());
        vo.setReason(entity.getReason());
        vo.setStatus(entity.getStatus());
        vo.setApprover(entity.getApprover());
        vo.setApproveComment(entity.getApproveComment());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}