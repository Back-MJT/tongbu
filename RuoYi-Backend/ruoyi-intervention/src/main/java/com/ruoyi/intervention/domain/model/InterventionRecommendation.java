package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.AdjustmentDimension;
import com.ruoyi.intervention.domain.enums.AdjustmentTriggerType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Intervention adjustment recommendation output by DynamicAdjustmentService.
 * Contains the set of adjustment actions triggered by health data signals.
 *
 * Reference: ACSM 2021 Ch.4; Nahum-Shani 2018 JITAI design patterns
 */
public class InterventionRecommendation {
    private String recommendationId;
    private String userId;
    private String interventionId;
    private List<AdjustmentAction> actions;
    private String triggerSummary;
    private String riskLevel;
    private List<String> triggeredByTriggers;
    private List<String> matchedRules;
    private double confidence;
    private List<String> evidenceRefs;
    private LocalDateTime createdAt;
    private boolean applied;
    private LocalDateTime appliedAt;

    public InterventionRecommendation() {
        this.createdAt = LocalDateTime.now();
        this.evidenceRefs = List.of();
        this.triggeredByTriggers = List.of();
        this.matchedRules = List.of();
    }

    public InterventionRecommendation(String recommendationId, String userId, String interventionId) {
        this();
        this.recommendationId = recommendationId;
        this.userId = userId;
        this.interventionId = interventionId;
    }

    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String v) { this.recommendationId = v; }
    public String getUserId() { return userId; }
    public void setUserId(String v) { this.userId = v; }
    public String getInterventionId() { return interventionId; }
    public void setInterventionId(String v) { this.interventionId = v; }
    public List<AdjustmentAction> getActions() { return actions; }
    public void setActions(List<AdjustmentAction> v) { this.actions = v; }
    public String getTriggerSummary() { return triggerSummary; }
    public void setTriggerSummary(String v) { this.triggerSummary = v; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String v) { this.riskLevel = v; }
    public List<String> getTriggeredByTriggers() { return triggeredByTriggers; }
    public void setTriggeredByTriggers(List<String> v) { this.triggeredByTriggers = v; }
    public List<String> getMatchedRules() { return matchedRules; }
    public void setMatchedRules(List<String> v) { this.matchedRules = v; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double v) { this.confidence = v; }
    public List<String> getEvidenceRefs() { return evidenceRefs; }
    public void setEvidenceRefs(List<String> v) { this.evidenceRefs = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public boolean isApplied() { return applied; }
    public void setApplied(boolean v) { this.applied = v; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime v) { this.appliedAt = v; }
}
