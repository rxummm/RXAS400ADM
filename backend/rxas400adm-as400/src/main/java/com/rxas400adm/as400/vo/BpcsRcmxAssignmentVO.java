package com.rxas400adm.as400.vo;

/**
 * ㊾ RCMX CSR 分配管理 VO。
 */
public record BpcsRcmxAssignmentVO(
        String cust,
        String custName,
        String csrId,
        String csrName,
        String active,
        String maintUser,
        String maintDate
) {
}
