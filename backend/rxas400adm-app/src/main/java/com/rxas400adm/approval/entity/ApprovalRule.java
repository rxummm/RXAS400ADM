package com.rxas400adm.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审批规则实体
 */
@Data
@TableName("rx_approval_rule")
public class ApprovalRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 目标类型: PO, INVOICE, OPERATION */
    private String targetType;

    /** 金额下限 */
    private BigDecimal amountMin;

    /** 金额上限 */
    private BigDecimal amountMax;

    /** 审批级别 */
    private Integer level;

    /** ROLE/USER */
    private String approverType;

    /** 审批人/角色ID */
    private Long approverId;

    /** 是否启用 */
    private Boolean enabled;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
