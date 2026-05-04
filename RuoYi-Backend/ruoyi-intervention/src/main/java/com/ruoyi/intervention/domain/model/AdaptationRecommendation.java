package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.AdaptationRule;
import com.ruoyi.intervention.domain.enums.FeedbackType;

import java.time.LocalDateTime;

/**
 * Adaptation recommendation from the feedback loop engine.
 * References: feedback_loop.py AdaptationRecommendation
 */
public class AdaptationRecommendation {
    private String recommendationId;
    private String userId;
    private AdaptationRule adaptationRule;
    private FeedbackType feedbackType;
    private double confidence;
    private String rationale;
    private String evidenceRef;
    private LocalDateTime createdAt;

    public AdaptationRecommendation() {
        this.createdAt = LocalDateTime.now();
    }

    public AdaptationRecommendation(String recommendationId, String userId,
            AdaptationRule adaptationRule, FeedbackType feedbackType,
            double confidence, String rationale, String evidenceRef) {
        this();
        this.recommendationId = recommendationId;
        this.userId = userId;
        this.adaptationRule = adaptationRule;
        this.feedbackType = feedbackType;
        this.confidence = confidence;
        this.rationale = rationale;
        this.evidenceRef = evidenceRef;
    }

    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public AdaptationRule getAdaptationRule() { return adaptationRule; }
    public void setAdaptationRule(AdaptationRule adaptationRule) { this.adaptationRule = adaptationRule; }
    public FeedbackType getFeedbackType() { return feedbackType; }
    public void setFeedbackType(FeedbackType feedbackType) { this.feedbackType = feedbackType; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public String getEvidenceRef() { return evidenceRef; }
    public void setEvidenceRef(String evidenceRef) { this.evidenceRef = evidenceRef; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
