package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.AnomalyType;

import java.time.LocalDateTime;

/**
 * An anomaly detected in a health metric time series via z-score analysis.
 *
 * Reference: Nahum-Shani et al. 2018 JITAI design patterns
 */
public record AnomalyDetectionResult(
    LocalDateTime timestamp,
    String metric,
    AnomalyType anomalyType,
    double observedValue,
    double expectedValue,
    double zScore,
    String severity,   // mild / moderate / severe
    String evidenceRef
) {}
