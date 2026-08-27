package com.rxas400adm.as400.vo;

import java.util.List;

/**
 * 【AS400 业务增强·P2】发运载荷 VO（LLH 载荷头 + LLM 载荷明细）。
 * LHSTAT: 0=Planned, 1=Firmed, 2=Released, 3=Dispatched
 */
public record BpcsLoadVO(
        String cono,
        String lhno,
        /** 载荷状态：0~3 */
        int status,
        /** 状态标签 key（bpcs.loadStatus.*） */
        String statusKey,
        /** 承运人 */
        String carrier,
        /** 目的地 */
        String destination,
        /** 发运日期 */
        String shipDate,
        /** 承载订单号列表 */
        List<String> orderNos,
        /** 承载行数 */
        int lineCount,
        /** 总重量 */
        double weight
) {}
