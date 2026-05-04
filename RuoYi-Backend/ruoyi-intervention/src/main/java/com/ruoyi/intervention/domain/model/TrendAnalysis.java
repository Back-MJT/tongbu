package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.TrendType;

/**
 * Result of trend analysis on a time-series sliding window.
 *
 * Reference: Nahum-Shani et al. 2018 JITAI design patterns
 */
public record TrendAnalysis(
    String metric,
    TrendType trend,
    /** Linear regression slope (units per day) */
    double slope,
    /** Fractional change from window start to end */
    double changeFraction,
    double windowStartValue,
    double windowEndValue,
    int nPoints,
    /** Statistical confidence 0.0-1.0 */
    double confidence,
    String evidenceRef
) {
    /**
     * Convenience: infer changeFraction and set confidence to 0 when insufficient data.
     */
    public TrendAnalysis(String metric, TrendType trend, double slope,
            double windowStartValue, double windowEndValue, int nPoints, String evidenceRef) {
        this(metric, trend, slope,
            windowStartValue != 0.0 ? (windowEndValue - windowStartValue) / windowStartValue : 0.0,
            windowStartValue, windowEndValue, nPoints, 0.0, evidenceRef);
    }
}
