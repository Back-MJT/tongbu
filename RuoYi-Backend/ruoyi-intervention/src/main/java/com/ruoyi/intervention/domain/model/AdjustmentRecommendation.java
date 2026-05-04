package com.ruoyi.intervention.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A complete adjustment recommendation containing one or more AdjustmentActions.
 * Represents the engine output: a proposed modification to an intervention plan.
 *
 * Reference: Nahum-Shani et al. 2018 JITAI design patterns
 */
public class AdjustmentRecommendation {
    private String recommendationId;
    private String userId;
    private String interventionId;
    private List<AdjustmentAction> actions = new ArrayList<>();
    private String triggerSummary;
    private String riskLevel;
    private List<String> triggeredByTriggers = new ArrayList<>();
    private List<String> matchedRules = new ArrayList<>();
    private double confidence;
    private List<String> evidenceRefs = new ArrayList<>();
    private LocalDateTime createdAt;
    private boolean applied;
    private LocalDateTime appliedAt;
    private LocalDateTime cancelledAt;

    public AdjustmentRecommendation() {
        this.createdAt = LocalDateTime.now();
        this.riskLevel = "low";
    }

    public AdjustmentRecommendation(String recommendationId, String userId, String interventionId) {
        this();
        this.recommendationId = recommendationId;
        this.userId = userId;
        this.interventionId = interventionId;
    }

    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getInterventionId() { return interventionId; }
    public void setInterventionId(String interventionId) { this.interventionId = interventionId; }
    public List<AdjustmentAction> getActions() { return actions; }
    public void setActions(List<AdjustmentAction> actions) { this.actions = actions; }
    public void addAction(AdjustmentAction action) { this.actions.add(action); }
    public String getTriggerSummary() { return triggerSummary; }
    public void setTriggerSummary(String triggerSummary) { this.triggerSummary = triggerSummary; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public List<String> getTriggeredByTriggers() { return triggeredByTriggers; }
    public void setTriggeredByTriggers(List<String> triggeredByTriggers) { this.triggeredByTriggers = triggeredByTriggers; }
    public List<String> getMatchedRules() { return matchedRules; }
    public void setMatchedRules(List<String> matchedRules) { this.matchedRules = matchedRules; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public List<String> getEvidenceRefs() { return evidenceRefs; }
    public void setEvidenceRefs(List<String> evidenceRefs) { this.evidenceRefs = evidenceRefs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
