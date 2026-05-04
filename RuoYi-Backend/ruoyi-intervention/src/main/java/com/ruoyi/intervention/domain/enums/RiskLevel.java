package com.ruoyi.intervention.domain.enums;

/**
 * Health risk severity levels for intervention safety.
 *
 * Reference: ACSM 2021 Ch.2 risk stratification
 */
public enum RiskLevel {
    LOW,      // No elevated risk detected
    MODERATE, // 1+ risk factors present
    HIGH,     // Significant risk requiring attention
    CRITICAL  // Immediate safety concern
}
