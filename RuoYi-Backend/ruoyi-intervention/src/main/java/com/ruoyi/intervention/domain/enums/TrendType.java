package com.ruoyi.intervention.domain.enums;

/**
 * Health trend direction from time-series analysis.
 *
 * Reference: Nahum-Shani et al. 2018: JITAI design patterns
 */
public enum TrendType {
    /** Statistically significant upward trend */
    IMPROVING,
    /** No significant change */
    STABLE,
    /** Statistically significant downward trend */
    DECLINING,
    /** Not enough data points for trend computation */
    INSUFFICIENT_DATA
}
