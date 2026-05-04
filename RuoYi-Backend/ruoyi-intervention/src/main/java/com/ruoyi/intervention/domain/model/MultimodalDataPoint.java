package com.ruoyi.intervention.domain.model;

import java.time.LocalDateTime;

/**
 * Unified multi-modal data snapshot for a single time period.
 *
 * Combines exercise, sleep, heart rate, and nutrition data from potentially
 * multiple sources (devices, self-report, prescriptions) into a single view.
 *
 * Reference: Nahum-Shani et al. 2018 JITAI design patterns
 */
public record MultimodalDataPoint(
    String userId,
    LocalDateTime timestamp,
    Double exerciseMinutes,
    Double exerciseIntensityAvg,
    Integer steps,
    Double sleepDurationHours,
    Integer sleepQualityScore,
    Integer restingHr,
    Double hrvRmssd,
    Double avgHr,
    Double proteinG,
    Double waterLiters,
    Double caloriesConsumed,
    Integer energyLevel,
    String evidenceRef
) {
    public MultimodalDataPoint(String userId, LocalDateTime timestamp) {
        this(userId, timestamp,
            null, null, null, null, null, null, null, null,
            null, null, null, null, "nahum_shani_2018_jitai_design");
    }

    public MultimodalDataPoint(String userId, LocalDateTime timestamp, String evidenceRef) {
        this(userId, timestamp,
            null, null, null, null, null, null, null, null,
            null, null, null, null, evidenceRef);
    }
}
