package com.ruoyi.intervention.domain.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 基线健康评分 — 各维度0-100分
 * Baseline health scores for different dimensions.
 *
 * Reference: ACSM 2021 Ch.4 fitness assessment
 */
public class BaselineScores
{
    @NotNull
    @Min(0) @Max(100)
    private Integer cardiovascular;

    @NotNull
    @Min(0) @Max(100)
    private Integer metabolic;

    @NotNull
    @Min(0) @Max(100)
    private Integer sleep;

    @NotNull
    @Min(0) @Max(100)
    private Integer musculoskeletal;

    public BaselineScores() {}

    public BaselineScores(Integer cardiovascular, Integer metabolic, Integer sleep, Integer musculoskeletal)
    {
        this.cardiovascular = cardiovascular;
        this.metabolic = metabolic;
        this.sleep = sleep;
        this.musculoskeletal = musculoskeletal;
    }

    // --- Getters & Setters ---

    public Integer getCardiovascular() { return cardiovascular; }
    public void setCardiovascular(Integer cardiovascular) { this.cardiovascular = cardiovascular; }

    public Integer getMetabolic() { return metabolic; }
    public void setMetabolic(Integer metabolic) { this.metabolic = metabolic; }

    public Integer getSleep() { return sleep; }
    public void setSleep(Integer sleep) { this.sleep = sleep; }

    public Integer getMusculoskeletal() { return musculoskeletal; }
    public void setMusculoskeletal(Integer musculoskeletal) { this.musculoskeletal = musculoskeletal; }

    /**
     * Calculate average score across all dimensions.
     */
    public int average()
    {
        return (cardiovascular + metabolic + sleep + musculoskeletal) / 4;
    }
}
