package com.rxas400adm.as400.vo;

/**
 * 订单变更 VO。
 */
public record OrderChangeVO(
        Long id,
        String cono,
        String orno,
        String changeType,
        String fieldName,
        String oldValue,
        String newValue,
        String reason,
        String changedBy,
        String changedTime
) {
    public static OrderChangeVO from(com.rxas400adm.as400.entity.OrderChange e) {
        return new OrderChangeVO(
                e.getId(), e.getCono(), e.getOrno(), e.getChangeType(),
                e.getFieldName(), e.getOldValue(), e.getNewValue(),
                e.getReason(), e.getChangedBy(), e.getChangedTime()
        );
    }
}
