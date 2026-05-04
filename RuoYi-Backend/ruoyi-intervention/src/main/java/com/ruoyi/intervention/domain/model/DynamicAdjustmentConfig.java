package com.ruoyi.intervention.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Configuration for the dynamic adjustment engine.
 *
 * Reference: Nahum-Shani et al. 2018 JITAI design; ACSM 2021 Ch.4
 */
public class DynamicAdjustmentConfig {
    // Trend detection
    private int trendWindowDays = 7;
    private int minTrendPoints = 3;
    private double trendSignificanceFraction = 0.15;

    // Anomaly detection
    private double anomalyZscoreTrigger = 2.0;

    // Safety limits
    private double maxSingleIntensityChange = 0.15;
    private double maxSingleDurationChangePct = 0.20;
    private int maxSingleFrequencyChange = 1;
    private double minIntensityFloor = 0.30;
    private double maxIntensityCeiling = 1.0;
    private int minSessionDurationMin = 5;
    private int maxSessionDurationMin = 120;

    // Feedback integration
    private boolean integrateFeedbackLoop = true;
    private double feedbackConfidenceThreshold = 0.5;

    // Stagnation detection
    private int stagnationWeeks = 4;

    // Cooldowns
    private int minAdjustmentIntervalHours = 24;

    // Progressive overload (ACSM)
    private double progressiveOverloadWeeklyIntensityIncrement = 0.025;
    private double progressiveOverloadWeeklyVolumeIncrement = 0.05;

    private List<String> evidenceRefs = List.of(
        "acsm_2021_ch4", "acsm_2021_ch7", "tanaka_2001_hrmax",
        "hirshkowitz_2015", "nahum_shani_2018_jitai_design",
        "klasnja_2015_jitai", "bomberg_2017_progressive_overload"
    );

    private Map<String, AdjustmentRuleSpec> presetRules;

    // --- Getters and setters ---
    public int getTrendWindowDays() { return trendWindowDays; }
    public void setTrendWindowDays(int trendWindowDays) { this.trendWindowDays = trendWindowDays; }
    public int getMinTrendPoints() { return minTrendPoints; }
    public void setMinTrendPoints(int minTrendPoints) { this.minTrendPoints = minTrendPoints; }
    public double getTrendSignificanceFraction() { return trendSignificanceFraction; }
    public void setTrendSignificanceFraction(double trendSignificanceFraction) { this.trendSignificanceFraction = trendSignificanceFraction; }
    public double getAnomalyZscoreTrigger() { return anomalyZscoreTrigger; }
    public void setAnomalyZscoreTrigger(double anomalyZscoreTrigger) { this.anomalyZscoreTrigger = anomalyZscoreTrigger; }
    public double getMaxSingleIntensityChange() { return maxSingleIntensityChange; }
    public void setMaxSingleIntensityChange(double maxSingleIntensityChange) { this.maxSingleIntensityChange = maxSingleIntensityChange; }
    public double getMaxSingleDurationChangePct() { return maxSingleDurationChangePct; }
    public void setMaxSingleDurationChangePct(double maxSingleDurationChangePct) { this.maxSingleDurationChangePct = maxSingleDurationChangePct; }
    public int getMaxSingleFrequencyChange() { return maxSingleFrequencyChange; }
    public void setMaxSingleFrequencyChange(int maxSingleFrequencyChange) { this.maxSingleFrequencyChange = maxSingleFrequencyChange; }
    public double getMinIntensityFloor() { return minIntensityFloor; }
    public void setMinIntensityFloor(double minIntensityFloor) { this.minIntensityFloor = minIntensityFloor; }
    public double getMaxIntensityCeiling() { return maxIntensityCeiling; }
    public void setMaxIntensityCeiling(double maxIntensityCeiling) { this.maxIntensityCeiling = maxIntensityCeiling; }
    public int getMinSessionDurationMin() { return minSessionDurationMin; }
    public void setMinSessionDurationMin(int minSessionDurationMin) { this.minSessionDurationMin = minSessionDurationMin; }
    public int getMaxSessionDurationMin() { return maxSessionDurationMin; }
    public void setMaxSessionDurationMin(int maxSessionDurationMin) { this.maxSessionDurationMin = maxSessionDurationMin; }
    public boolean isIntegrateFeedbackLoop() { return integrateFeedbackLoop; }
    public void setIntegrateFeedbackLoop(boolean integrateFeedbackLoop) { this.integrateFeedbackLoop = integrateFeedbackLoop; }
    public double getFeedbackConfidenceThreshold() { return feedbackConfidenceThreshold; }
    public void setFeedbackConfidenceThreshold(double feedbackConfidenceThreshold) { this.feedbackConfidenceThreshold = feedbackConfidenceThreshold; }
    public int getStagnationWeeks() { return stagnationWeeks; }
    public void setStagnationWeeks(int stagnationWeeks) { this.stagnationWeeks = stagnationWeeks; }
    public int getMinAdjustmentIntervalHours() { return minAdjustmentIntervalHours; }
    public void setMinAdjustmentIntervalHours(int minAdjustmentIntervalHours) { this.minAdjustmentIntervalHours = minAdjustmentIntervalHours; }
    public double getProgressiveOverloadWeeklyIntensityIncrement() { return progressiveOverloadWeeklyIntensityIncrement; }
    public void setProgressiveOverloadWeeklyIntensityIncrement(double v) { this.progressiveOverloadWeeklyIntensityIncrement = v; }
    public double getProgressiveOverloadWeeklyVolumeIncrement() { return progressiveOverloadWeeklyVolumeIncrement; }
    public void setProgressiveOverloadWeeklyVolumeIncrement(double v) { this.progressiveOverloadWeeklyVolumeIncrement = v; }
    public List<String> getEvidenceRefs() { return evidenceRefs; }
    public void setEvidenceRefs(List<String> evidenceRefs) { this.evidenceRefs = evidenceRefs; }
    public Map<String, AdjustmentRuleSpec> getPresetRules() { return presetRules; }
    public void setPresetRules(Map<String, AdjustmentRuleSpec> presetRules) { this.presetRules = presetRules; }
}
