package com.ruoyi.intervention.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of multimodal fusion scoring across all four health dimensions.
 *
 * Reference: WHO 2020 PA; AASM 2020; Karvonen 1957; Nahum-Shani 2018 JITAI
 */
public record MultimodalFusionReport(
    String userId,
    LocalDateTime assessedAt,
    DimensionFusionScore exerciseScore,
    DimensionFusionScore sleepScore,
    DimensionFusionScore heartRateScore,
    DimensionFusionScore nutritionScore,
    /** Weighted composite across all dimensions 0-100 */
    double compositeScore,
    String compositeGrade,
    String compositeGradeLabelZh,
    String configVersion,
    List<String> evidenceRefs
) {}
