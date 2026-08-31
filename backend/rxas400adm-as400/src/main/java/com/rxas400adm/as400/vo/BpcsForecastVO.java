package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预测补货看板 VO。
 */
public record BpcsForecastVO(
        List<MonthlyDemand> monthlyDemand,
        List<StockLevel> stockLevels,
        List<ReplenishSuggestion> replenishSuggestions,
        ForecastMetrics metrics
) {
    /** 月度需求数据点 */
    public record MonthlyDemand(
            String ym,
            int actual,
            int forecast,
            int upperBound,
            int lowerBound
    ) {}

    /** 库存水平数据点 */
    public record StockLevel(
            String ym,
            int onHand,
            int safetyStock
    ) {}

    /** 补货建议 */
    public record ReplenishSuggestion(
            String item,
            int currentStock,
            int suggestedOrder
    ) {}

    /** 预测准确度指标 */
    public record ForecastMetrics(
            BigDecimal mape,
            BigDecimal bias,
            BigDecimal gmAbc,
            BigDecimal gmXyz
    ) {}
}
