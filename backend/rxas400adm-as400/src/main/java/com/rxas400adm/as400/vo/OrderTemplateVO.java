package com.rxas400adm.as400.vo;

/**
 * 订单模板 VO。
 */
public record OrderTemplateVO(
        Long id,
        String templateName,
        String cono,
        String cust,
        String shipTo,
        String remark,
        String lineJson,
        int useCount,
        String active,
        String createdBy
) {
    public static OrderTemplateVO from(com.rxas400adm.as400.entity.OrderTemplate e) {
        return new OrderTemplateVO(
                e.getId(), e.getTemplateName(), e.getCono(), e.getCust(),
                e.getShipTo(), e.getRemark(), e.getLineJson(),
                e.getUseCount() != null ? e.getUseCount() : 0,
                e.getActive(), e.getCreatedBy()
        );
    }
}
