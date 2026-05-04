package com.ruoyi.intervention.domain.model;

/**
 * 阶段判定阈值 — 可配置的阶段转换阈值参数.
 * Configurable thresholds for stage determination.
 *
 * Defaults from Board v1.1 Section 2.3:
 *   Beginner: max 4 weeks, max freq 3/week, max completion 60%
 *   Growth: min completion 70%, min freq 2/week, max weeks 12
 *   Plateau: 7 days inactivity, intensity decline < -0.05
 *   Advanced: min 12 weeks, min freq 3/week, min intensity trend 0.02
 */
public class StageThresholds
{
    // Beginner stage
    private double beginnerMaxWeeks = 4.0;
    private double beginnerMaxFrequency = 3.0;
    private double beginnerMaxCompletion = 0.6;

    // Growth stage
    private double growthMinCompletion = 0.7;
    private double growthMinFrequency = 2.0;
    private double growthMaxWeeks = 12.0;

    // Plateau triggers
    private int plateauInactivityDays = 7;
    private double plateauIntensityDecline = -0.05;

    // Advanced stage
    private double advancedMinWeeks = 12.0;
    private double advancedMinFrequency = 3.0;
    private double advancedMinIntensityTrend = 0.02;

    public StageThresholds() {}

    // --- Getters & Setters ---

    public double getBeginnerMaxWeeks() { return beginnerMaxWeeks; }
    public void setBeginnerMaxWeeks(double v) { this.beginnerMaxWeeks = v; }

    public double getBeginnerMaxFrequency() { return beginnerMaxFrequency; }
    public void setBeginnerMaxFrequency(double v) { this.beginnerMaxFrequency = v; }

    public double getBeginnerMaxCompletion() { return beginnerMaxCompletion; }
    public void setBeginnerMaxCompletion(double v) { this.beginnerMaxCompletion = v; }

    public double getGrowthMinCompletion() { return growthMinCompletion; }
    public void setGrowthMinCompletion(double v) { this.growthMinCompletion = v; }

    public double getGrowthMinFrequency() { return growthMinFrequency; }
    public void setGrowthMinFrequency(double v) { this.growthMinFrequency = v; }

    public double getGrowthMaxWeeks() { return growthMaxWeeks; }
    public void setGrowthMaxWeeks(double v) { this.growthMaxWeeks = v; }

    public int getPlateauInactivityDays() { return plateauInactivityDays; }
    public void setPlateauInactivityDays(int v) { this.plateauInactivityDays = v; }

    public double getPlateauIntensityDecline() { return plateauIntensityDecline; }
    public void setPlateauIntensityDecline(double v) { this.plateauIntensityDecline = v; }

    public double getAdvancedMinWeeks() { return advancedMinWeeks; }
    public void setAdvancedMinWeeks(double v) { this.advancedMinWeeks = v; }

    public double getAdvancedMinFrequency() { return advancedMinFrequency; }
    public void setAdvancedMinFrequency(double v) { this.advancedMinFrequency = v; }

    public double getAdvancedMinIntensityTrend() { return advancedMinIntensityTrend; }
    public void setAdvancedMinIntensityTrend(double v) { this.advancedMinIntensityTrend = v; }
}
