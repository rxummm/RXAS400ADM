package com.rxas400adm.procurement.vo;

import com.rxas400adm.procurement.entity.PurchaseApproval;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采购审批记录 VO。
 */
@Data
public class PurchaseApprovalVO {

    private Long id;
    private Long poId;
    private Integer level;
    private String approver;
    private String action;
    private String comment;
    private LocalDateTime actionTime;

    public static PurchaseApprovalVO from(PurchaseApproval entity) {
        PurchaseApprovalVO vo = new PurchaseApprovalVO();
        vo.setId(entity.getId());
        vo.setPoId(entity.getPoId());
        vo.setLevel(entity.getLevel());
        vo.setApprover(entity.getApprover());
        vo.setAction(entity.getAction());
        vo.setComment(entity.getComment());
        vo.setActionTime(entity.getActionTime());
        return vo;
    }
}