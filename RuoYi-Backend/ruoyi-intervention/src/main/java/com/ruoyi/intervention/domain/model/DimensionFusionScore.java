package com.ruoyi.intervention.domain.model;

import java.util.Map;

/**
 * Score for a single dimension (exercise/sleep/HR/nutrition) from multimodal fusion.
 *
 * Reference: WHO 2020 PA guidelines; ACSM 2021 Ch.4; Hirshkowitz 2015 NSF sleep
 */
public record DimensionFusionScore(
    String dimension,
    /** Raw score 0-100 */
    double rawScore,
    /** Grade tier: excellent / good / fair / poor */
    String grade,
    String gradeLabelZh,
    Map<String, Double> subScores,
    String evidenceRef
) {}
