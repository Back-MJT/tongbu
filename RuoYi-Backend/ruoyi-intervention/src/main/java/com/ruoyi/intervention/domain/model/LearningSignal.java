package com.ruoyi.intervention.domain.model;

import java.util.Date;

/**
 * Aggregated learning signal across multiple feedback events.
 * Used to detect patterns and update population-level priors.
 */
public class LearningSignal
{
    private String signalId;
    private String userId;
    /** Health dimension: cardiovascular / metabolic / sleep / musculoskeletal */
    private String dimension;
    /** Type: compliance_trend / rpe_trend / outcome_trend / adverse_signal */
    private String signalType;
    /** Trend direction: improving / stable / worsening */
    private String direction;
    /** Aggregated metric value */
    private Double value;
    private Integer nObservations;
    /** Confidence 0-1 */
    private Double confidence;
    private Date derivedAt;
    private String evidenceRef = "liao_2020_micro_randomized_trials";

    public LearningSignal()
    {
        this.derivedAt = new Date();
    }

    // --- Getters and Setters ---
    public String getSignalId() { return signalId; }
    public void setSignalId(String signalId) { this.signalId = signalId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public String getSignalType() { return signalType; }
    public void setSignalType(String signalType) { this.signalType = signalType; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public Integer getNObservations() { return nObservations; }
    public void setNObservations(Integer nObservations) { this.nObservations = nObservations; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Date getDerivedAt() { return derivedAt; }
    public void setDerivedAt(Date derivedAt) { this.derivedAt = derivedAt; }

    public String getEvidenceRef() { return evidenceRef; }
    public void setEvidenceRef(String evidenceRef) { this.evidenceRef = evidenceRef; }
}
