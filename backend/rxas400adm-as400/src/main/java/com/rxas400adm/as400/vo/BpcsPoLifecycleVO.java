package com.rxas400adm.as400.vo;

/**
 * ⑤ PO 全生命周期 VO。
 */
public record BpcsPoLifecycleVO(
        String cono,
        String poNo,
        String vendor,
        String vendorName,
        String orderDate,
        String receivedDate,
        String status,
        boolean onHold,
        Integer lineCount
) {
}
