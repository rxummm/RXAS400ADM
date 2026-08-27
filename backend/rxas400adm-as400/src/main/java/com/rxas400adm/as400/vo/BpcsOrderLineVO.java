package com.rxas400adm.as400.vo;

import java.math.BigDecimal;

/**
 * 【AS400 业务增强·P1】客户订单行 VO。
 *
 * <p>数量字段随阶段演进产生：未到达的阶段对应数量为 null（前端显示 “—”），
 * 由行级 stageIndex 驱动表格动态列与抽屉分组展示。
 */
public record BpcsOrderLineVO(
        String orln,
        String item,
        String itemDesc,
        String wh,
        Integer qtyOrdered,
        Integer qtyAllocated,
        Integer qtyShipped,
        Integer qtyInvoiced,
        BigDecimal price,
        BigDecimal discPct,
        String reqDate,
        /** 行当前阶段下标（0=录入 1=拣货释放 2=拣货确认 3=开票 4=关闭；发运两位恒 0 不参与） */
        int stageIndex,
        String stageKey,
        String rawClsts) {
}
