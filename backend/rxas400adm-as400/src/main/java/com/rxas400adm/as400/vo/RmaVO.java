package com.rxas400adm.as400.vo;

import com.rxas400adm.as400.entity.Rma;

/**
 * 退货 RMA VO。
 */
public record RmaVO(
        Long id,
        String rmaNo,
        String cono,
        String orno,
        String cust,
        String item,
        int qty,
        String reason,
        String status,
        String createdBy,
        String createdTime
) {
    public static RmaVO from(Rma e) {
        return new RmaVO(
                e.getId(), e.getRmaNo(), e.getCono(), e.getOrno(),
                e.getCust(), e.getItem(), e.getQty() != null ? e.getQty() : 0,
                e.getReason(), e.getStatus(), e.getCreatedBy(), e.getCreatedTime()
        );
    }
}
