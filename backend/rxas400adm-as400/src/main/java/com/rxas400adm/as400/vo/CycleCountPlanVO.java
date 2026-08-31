package com.rxas400adm.as400.vo;

/**
 * 循环盘点计划 VO。
 */
public record CycleCountPlanVO(
        Long id,
        String planNo,
        String item,
        String itemDesc,
        String warehouse,
        String plannedDate,
        String status,
        String abcClass,
        String operator,
        String createdBy,
        String createdTime
) {
}
