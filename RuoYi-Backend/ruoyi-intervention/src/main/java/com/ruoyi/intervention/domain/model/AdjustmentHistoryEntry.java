package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.AdjustmentDimension;
import com.ruoyi.intervention.domain.enums.AdjustmentTriggerType;

import java.time.LocalDateTime;

/**
 * A single entry in the adjustment history log.
 * Records what was adjusted, when, and why — for full auditability.
 *
 * Reference: Nahum-Shani et al. 2018 JITAI design patterns
 */
public class AdjustmentHistoryEntry {
    private String entryId;
    private String userId;
    private String interventionId;
    private String recommendationId;
    private AdjustmentDimension dimension;
    private String parameterName;
    private Object oldValue;
    private Object newValue;
    private double changeFraction;
    private AdjustmentTriggerType triggerType;
    private String triggerId;
    private String ruleId;
    private String rationale;
    private String evidenceRef;
    private double confidence;
    private LocalDateTime timestamp;

    public AdjustmentHistoryEntry() {
        this.timestamp = LocalDateTime.now();
    }

    public AdjustmentHistoryEntry(String entryId, String userId, String interventionId,
            String recommendationId, AdjustmentDimension dimension, String parameterName,
            Object oldValue, Object newValue, double changeFraction,
            AdjustmentTriggerType triggerType, String triggerId, String ruleId,
            String rationale, String evidenceRef, double confidence) {
        this();
        this.entryId = entryId;
        this.userId = userId;
        this.interventionId = interventionId;
        this.recommendationId = recommendationId;
        this.dimension = dimension;
        this.parameterName = parameterName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeFraction = changeFraction;
        this.triggerType = triggerType;
        this.triggerId = triggerId;
        this.ruleId = ruleId;
        this.rationale = rationale;
        this.evidenceRef = evidenceRef;
        this.confidence = confidence;
    }

    public String getEntryId() { return entryId; }
    public void setEntryId(String v) { this.entryId = v; }
    public String getUserId() { return userId; }
    public void setUserId(String v) { this.userId = v; }
    public String getInterventionId() { return interventionId; }
    public void setInterventionId(String v) { this.interventionId = v; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String v) { this.recommendationId = v; }
    public AdjustmentDimension getDimension() { return dimension; }
    public void setDimension(AdjustmentDimension v) { this.dimension = v; }
    public String getParameterName() { return parameterName; }
    public void setParameterName(String v) { this.parameterName = v; }
    public Object getOldValue() { return oldValue; }
    public void setOldValue(Object v) { this.oldValue = v; }
    public Object getNewValue() { return newValue; }
    public void setNewValue(Object v) { this.newValue = v; }
    public double getChangeFraction() { return changeFraction; }
    public void setChangeFraction(double v) { this.changeFraction = v; }
    public AdjustmentTriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(AdjustmentTriggerType v) { this.triggerType = v; }
    public String getTriggerId() { return triggerId; }
    public void setTriggerId(String v) { this.triggerId = v; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String v) { this.ruleId = v; }
    public String getRationale() { return rationale; }
    public void setRationale(String v) { this.rationale = v; }
    public String getEvidenceRef() { return evidenceRef; }
    public void setEvidenceRef(String v) { this.evidenceRef = v; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double v) { this.confidence = v; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime v) { this.timestamp = v; }
}
