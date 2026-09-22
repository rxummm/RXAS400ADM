package com.rxas400adm.approval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审批通知 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalNotificationVO {
    private Long id;
    private String title;
    private String content;
    private String targetType;
    private Long targetId;
    private Long approverId;
    private String approverName;
    private String status;
    private String action;
    private String comment;
    private String createdBy;
    private LocalDateTime createdTime;

    public static ApprovalNotificationVO from(com.rxas400adm.approval.entity.ApprovalNotification entity) {
        ApprovalNotificationVO vo = new ApprovalNotificationVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        // 注意：ApprovalNotification 本身无 content 字段，保留用于前端展示兼容
        vo.setContent(entity.getContent());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        // 注意：approverId 保留展示用途，不建议外部写入
        vo.setApproverId(entity.getApproverId());
        vo.setApproverName(entity.getApproverName());
        vo.setStatus(entity.getStatus());
        // action 由 DTO 写入，不作为实体持久字段直接映射
        vo.setAction(entity.getAction());
        vo.setComment(entity.getComment());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }
}
