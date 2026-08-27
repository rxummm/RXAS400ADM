package com.rxas400adm.as400.vo;

import java.util.List;

/**
 * 订单全链路追踪 VO（订单→库存分配→发运→签收）。
 */
public record BpcsOrderTrackingVO(
        String cono,
        String orno,
        String custNo,
        String custName,
        String statusLabel,
        List<TrackingLine> lines) {

    /**
     * 追踪行（订单行 + 发运信息）。
     */
    public record TrackingLine(
            String item,
            String itemDesc,
            int qtyOrdered,
            int qtyAllocated,
            int qtyShipped,
            int qtyInvoiced,
            String shipDate,
            String shipStatus) {
    }
}
