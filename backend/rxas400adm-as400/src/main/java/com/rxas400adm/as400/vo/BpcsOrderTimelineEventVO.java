package com.rxas400adm.as400.vo;

/**
 * 订单历史事件时间线 VO（⑰ 订单详情增强）。
 */
public record BpcsOrderTimelineEventVO(
        /** 事件类型：CREATED / SHIPPED / INVOICED */
        String eventType,
        /** 事件日期 */
        String eventDate,
        /** 事件描述 */
        String eventDesc
) {
}
