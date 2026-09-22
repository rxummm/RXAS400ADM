package com.rxas400adm.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批通知实体
 */
@Data
@TableName("rx_approval_notification")
public class ApprovalNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 目标类型: PO, INVOICE, OPERATION */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** PENDING/APPROVED/REJECTED/CANCELLED */
    private String status;

    /** APPROVED/REJECTED/RETURNED */
    private String action;

    /** 审批意见 */
    private String comment;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
