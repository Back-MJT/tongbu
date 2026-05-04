package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.RiskLevel;
import com.ruoyi.intervention.domain.enums.TrendType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A versioned snapshot of a user's dynamic health profile.
 * Used for: comparison over time, rollback, audit trail, A/B testing cohorts.
 *
 * Reference: Nahum-Shani et al. 2018 JITAI design patterns
 */
public record ProfileVersion(
    String versionId,
    String userId,
    int versionNumber,
    LocalDateTime createdAt,
    double overallScore,
    Double compositeFusionScore,
    RiskLevel riskLevel,
    TrendType overallTrend,
    Integer restingHrSnapshot,
    Integer stepsSnapshot,
    Double sleepHoursSnapshot,
    List<String> changelog,
    String evidenceRef
) {}
