package com.rxas400adm.as400.vo;

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
    public static RmaVO from(com.rxas400adm.as400.entity.Rma e) {
        return new RmaVO(
                e.getId(), e.getRmaNo(), e.getCono(), e.getOrno(),
                e.getCust(), e.getItem(), e.getQty() != null ? e.getQty() : 0,
                e.getReason(), e.getStatus(), e.getCreatedBy(), e.getCreatedTime()
        );
    }
}
