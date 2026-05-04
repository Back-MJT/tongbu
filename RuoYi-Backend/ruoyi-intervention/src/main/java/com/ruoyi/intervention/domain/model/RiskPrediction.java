package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.RiskLevel;
import com.ruoyi.intervention.domain.enums.TrendType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Health risk prediction based on current profile state and trends.
 *
 * Reference: ACSM 2021 Ch.2 risk stratification
 */
public record RiskPrediction(
    String userId,
    LocalDateTime predictedAt,
    double currentRiskScore,
    RiskLevel currentRiskLevel,
    /** Predicted risk score in 30 days if current trend continues */
    Double predictedRiskScore30d,
    TrendType riskTrajectory,
    List<String> contributingFactors,
    boolean escalationRecommended,
    String escalationReason,
    double confidence,
    String evidenceRef
) {}
