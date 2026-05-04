package com.ruoyi.intervention.domain.enums;

/**
 * Categories of detected health anomalies.
 *
 * Reference: Rosner 1983 ESD outlier detection; Nahum-Shani 2018 JITAI
 */
public enum AnomalyType {
    /** Sudden sharp increase (e.g., HR spike) */
    SPIKE,
    /** Sudden sharp decrease (e.g., activity drop) */
    DROP,
    /** Value outside expected statistical range */
    OUTLIER,
    /** Pattern discontinuity in time series */
    TREND_BREAK
}
