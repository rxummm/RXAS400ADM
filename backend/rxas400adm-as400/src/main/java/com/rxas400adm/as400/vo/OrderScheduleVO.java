package com.rxas400adm.as400.vo;

/**
 * 订单排程 VO。
 */
public record OrderScheduleVO(
        Long id,
        String cono,
        String orno,
        String cust,
        String startDate,
        String endDate,
        int progress,
        int priority,
        String createdBy
) {
    public static OrderScheduleVO from(com.rxas400adm.as400.entity.OrderSchedule e) {
        return new OrderScheduleVO(
                e.getId(), e.getCono(), e.getOrno(), e.getCust(),
                e.getStartDate(), e.getEndDate(),
                e.getProgress() != null ? e.getProgress() : 0,
                e.getPriority() != null ? e.getPriority() : 5,
                e.getCreatedBy()
        );
    }
}
