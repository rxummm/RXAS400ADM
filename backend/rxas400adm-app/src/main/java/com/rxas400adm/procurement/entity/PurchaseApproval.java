package com.rxas400adm.procurement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采购审批记录表（rx_purchase_approval）。
 * 记录每一级审批的操作详情。
 */
@Data
@TableName("rx_purchase_approval")
public class PurchaseApproval {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 采购订单 ID */
    private Long poId;

    /** 审批层级（1=采购员, 2=经理, 3=财务） */
    private Integer level;

    /** 审批人 */
    private String approver;

    /** 动作：APPROVED / REJECTED / RETURNED */
    private String action;

    /** 审批意见 */
    private String comment;

    /** 操作时间 */
    private LocalDateTime actionTime;
}