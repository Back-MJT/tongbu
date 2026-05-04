package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.AdjustmentDimension;
import com.ruoyi.intervention.domain.enums.AdjustmentTriggerType;

import java.time.LocalDateTime;

/**
 * An event or condition that could trigger an intervention adjustment.
 * Produced by the engine after evaluating health data, anomalies, or feedback.
 *
 * Reference: Nahum-Shani et al. 2018 JITAI design patterns
 */
public class AdjustmentTrigger {
    private String triggerId;
    private AdjustmentTriggerType triggerType;
    private String userId;
    private AdjustmentDimension dimension;
    private TrendAnalysis trend;
    private AnomalyDetectionResult anomaly;
    private AdaptationRecommendation adaptation;
    private String reason;
    private double confidence;
    private LocalDateTime timestamp;
    private String evidenceRef;

    public AdjustmentTrigger() {
        this.timestamp = LocalDateTime.now();
        this.evidenceRef = "nahum_shani_2018_jitai_design";
    }

    // Convenience constructors matching service call sites

    // For TREND trigger
    public AdjustmentTrigger(String triggerId, AdjustmentTriggerType triggerType,
            String userId, AdjustmentDimension dimension,
            TrendAnalysis trend, AnomalyDetectionResult anomaly,
            AdaptationRecommendation adaptation, String reason,
            double confidence, String evidenceRef) {
        this();
        this.triggerId = triggerId;
        this.triggerType = triggerType;
        this.userId = userId;
        this.dimension = dimension;
        this.trend = trend;
        this.anomaly = anomaly;
        this.adaptation = adaptation;
        this.reason = reason;
        this.confidence = confidence;
        this.evidenceRef = evidenceRef;
    }

    // For ANOMALY trigger (without trend)
    public static AdjustmentTrigger forAnomaly(String triggerId, String userId,
            AdjustmentDimension dimension, AnomalyDetectionResult anomaly,
            double confidence, String evidenceRef) {
        AdjustmentTrigger t = new AdjustmentTrigger();
        t.triggerId = triggerId;
        t.triggerType = AdjustmentTriggerType.ANOMALY;
        t.userId = userId;
        t.dimension = dimension;
        t.anomaly = anomaly;
        t.confidence = confidence;
        t.evidenceRef = evidenceRef;
        return t;
    }

    // For FEEDBACK trigger
    public static AdjustmentTrigger forFeedback(String triggerId, String userId,
            AdjustmentDimension dimension, AdaptationRecommendation adaptation,
            double confidence, String evidenceRef) {
        AdjustmentTrigger t = new AdjustmentTrigger();
        t.triggerId = triggerId;
        t.triggerType = AdjustmentTriggerType.FEEDBACK;
        t.userId = userId;
        t.dimension = dimension;
        t.adaptation = adaptation;
        t.confidence = confidence;
        t.evidenceRef = evidenceRef;
        return t;
    }

    // For stagnation trigger
    public static AdjustmentTrigger forStagnation(String triggerId, String userId,
            AdjustmentDimension dimension, String reason, double confidence,
            String evidenceRef) {
        AdjustmentTrigger t = new AdjustmentTrigger();
        t.triggerId = triggerId;
        t.triggerType = AdjustmentTriggerType.TREND;
        t.userId = userId;
        t.dimension = dimension;
        t.reason = reason;
        t.confidence = confidence;
        t.evidenceRef = evidenceRef;
        return t;
    }

    // Getters & Setters
    public String getTriggerId() { return triggerId; }
    public void setTriggerId(String v) { this.triggerId = v; }
    public AdjustmentTriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(AdjustmentTriggerType v) { this.triggerType = v; }
    public String getUserId() { return userId; }
    public void setUserId(String v) { this.userId = v; }
    public AdjustmentDimension getDimension() { return dimension; }
    public void setDimension(AdjustmentDimension v) { this.dimension = v; }
    public TrendAnalysis getTrend() { return trend; }
    public void setTrend(TrendAnalysis v) { this.trend = v; }
    public AnomalyDetectionResult getAnomaly() { return anomaly; }
    public void setAnomaly(AnomalyDetectionResult v) { this.anomaly = v; }
    public AdaptationRecommendation getAdaptation() { return adaptation; }
    public void setAdaptation(AdaptationRecommendation v) { this.adaptation = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double v) { this.confidence = v; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime v) { this.timestamp = v; }
    public String getEvidenceRef() { return evidenceRef; }
    public void setEvidenceRef(String v) { this.evidenceRef = v; }
}
