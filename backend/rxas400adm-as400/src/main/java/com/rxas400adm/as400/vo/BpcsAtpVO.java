package com.rxas400adm.as400.vo;

import java.util.List;

/**
 * ATP（Available to Promise）可承诺发货 VO。
 * 支持时序 ATP 计算、订单行级承诺日期、ATP-OTIF 偏差分析。
 */
public record BpcsAtpVO(
        AtpSummary summary,
        List<AtpTimePhased> timePhased,
        List<AtpLinePromise> linePromises,
        List<AtpDeviation> deviations
) {
    /** ATP 汇总指标 */
    public record AtpSummary(
            int totalItems,
            int atpSufficient,       // ATP 充足的物料数
            int atpShortage,         // ATP 不足的物料数
            double overallFillRate,  // 总体可承诺率 %
            double avgPromiseDays    // 平均承诺提前天数
    ) {}

    /** 时序 ATP（按周/月） */
    public record AtpTimePhased(
            String period,           // 周期标签，如 "2026-W36" 或 "2026-09"
            long onHand,             // 期初库存
            long plannedReceipt,     // 计划入库（PO + MO）
            long committedDemand,    // 已承诺订单需求
            long atpQty,             // 该周期 ATP 余量
            long cumAtpQty,          // 累计 ATP
            String remark            // 备注
    ) {}

    /** 订单行级承诺日期 */
    public record AtpLinePromise(
            String cono,
            String orno,
            int lineNo,
            String item,
            String itemDesc,
            long requestedQty,
            String requestedDate,
            long availableNowQty,    // 当前可承诺量
            String earliestDate,     // 最早可承诺日期
            boolean canFulfillNow,   // 当前能否满足
            int leadTimeDays,        // 供应提前期（天）
            String promiseStatus     // CONFIRMED / PARTIAL / DELAYED
    ) {}

    /** ATP vs OTIF 偏差分析 */
    public record AtpDeviation(
            String item,
            String itemDesc,
            String partyCode,
            String partyName,
            double atpAccuracy,      // ATP 准确率 = (承诺日期兑现数 / 总承诺数) × 100
            double otifRate,         // 该维度 OTIF
            double deviationPct,     // 偏差 = OTIF - ATP 准确率
            int totalPromises,       // 总承诺次数
            int fulfilledOnTime,     // 准时兑现次数
            String rootCause,        // 根因标签
            String recommendation    // 改进建议
    ) {}
}
