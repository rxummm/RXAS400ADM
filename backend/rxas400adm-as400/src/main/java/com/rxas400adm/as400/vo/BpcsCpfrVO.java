package com.rxas400adm.as400.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 协同需求预测（CPFR）增强 VO。
 * 包含季节性分解、准确率回溯、多方协作预测。
 */
public record BpcsCpfrVO(
        SeasonalDecomposition seasonal,
        List<ForecastAccuracyBacktest> accuracyBacktest,
        List<CollaborativeForecast> collaborativeForecasts,
        CpfMetrics metrics
) {
    /** 季节性分解 */
    public record SeasonalDecomposition(
            List<MonthlyComponent> components,
            List<Double> seasonalIndices,
            String dominantSeason
    ) {}

    /** 月度分解成分 */
    public record MonthlyComponent(
            String ym,
            int actual,
            int trend,
            int seasonal,
            int residual,
            int deseasonalized
    ) {}

    /** 预测准确率回溯分析 */
    public record ForecastAccuracyBacktest(
            String ym,
            int actual,
            int predicted,
            int mapePct,
            int biasPct,
            String accuracyGrade
    ) {}

    /** 协同预测条目（多方参与） */
    public record CollaborativeForecast(
            String ym,
            int salesForecast,
            int marketingForecast,
            int supplyForecast,
            int consensusForecast,
            int finalActual,
            int consensusDeviation
    ) {}

    /** CPFR 综合指标 */
    public record CpfMetrics(
            BigDecimal forecastAccuracy,
            BigDecimal bias,
            BigDecimal seasonalStrength,
            BigDecimal collaborativeAlignment,
            BigDecimal overallScore
    ) {}
}
