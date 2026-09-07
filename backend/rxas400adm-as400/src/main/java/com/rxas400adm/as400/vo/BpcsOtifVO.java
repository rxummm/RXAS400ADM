package com.rxas400adm.as400.vo;

import java.util.List;

/**
 * OTIF（准时足量交付率）追踪 VO。
 * 包含汇总指标、按客户/供应商维度分解、月度趋势。
 */
public record BpcsOtifVO(
        OtifSummary summary,
        List<OtifByParty> byCustomer,
        List<OtifByParty> bySupplier,
        List<OtifTrend> monthlyTrend
) {
    /** OTIF 汇总指标 */
    public record OtifSummary(
            int totalShipments,
            int otifCompliant,
            double otifRate,
            double avgLeadTimeDays,
            double fillRatePct,
            int lateShipments,
            int shortShipments,
            int totalDisruptions
    ) {}

    /** 按客户/供应商维度的 OTIF 分解 */
    public record OtifByParty(
            String partyCode,
            String partyName,
            int totalOrders,
            int onTimeInFull,
            double otifRate,
            double avgLeadTimeDays,
            String rating
    ) {}

    /** 月度 OTIF 趋势 */
    public record OtifTrend(
            String ym,
            double otifRate,
            int totalShipments,
            int compliantCount,
            int lateCount,
            int shortCount
    ) {}
}
