package com.ruoyi.intervention.domain.enums;

/**
 * Categories of triggers that can cause an intervention adjustment.
 *
 * Reference: Nahum-Shani et al. 2018 JITAI design; Klasnja et al. 2015 JITAI theory
 */
public enum AdjustmentTriggerType {
    /** Slow trend-based change (7-day sliding window) */
    TREND,
    /** Sudden anomaly event (z-score based) */
    ANOMALY,
    /** Feedback-loop adaptation signal */
    FEEDBACK,
    /** Time-based scheduled review */
    SCHEDULED,
    /** Clinician-initiated manual override */
    MANUAL
}
