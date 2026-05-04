package com.ruoyi.intervention.domain.model;

import java.util.Date;
import com.ruoyi.intervention.domain.enums.ComplianceStatus;
import com.ruoyi.intervention.domain.enums.FeedbackType;

/**
 * A single feedback record from a user about an intervention event.
 */
public class FeedbackEntry
{
    private String feedbackId;
    private String userId;
    private String interventionId;
    private FeedbackType feedbackType;
    private Date timestamp;

    // Compliance
    private ComplianceStatus complianceStatus;
    /** Compliance ratio: proportion completed (0.0-1.0) */
    private Double complianceDetail;

    // Subjective
    /** Rate of Perceived Exertion (Borg 1-10) */
    private Integer rpe;
    /** Coded outcome: energized / neutral / fatigued */
    private String subjectiveOutcome;
    /** Energy level 1h post-intervention (1-10) */
    private Integer energyLevel;

    // Adverse events
    private Boolean adverseEvent = false;
    private String adverseEventDescription;
    /** Severity 1=mild, 5=severe */
    private Integer adverseEventSeverity;

    // Context
    private String contextNote;
    private String evidenceRef = "klasnja_2015_jitai";

    public FeedbackEntry()
    {
        this.timestamp = new Date();
        this.adverseEvent = false;
    }

    // --- Getters and Setters ---
    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getInterventionId() { return interventionId; }
    public void setInterventionId(String interventionId) { this.interventionId = interventionId; }

    public FeedbackType getFeedbackType() { return feedbackType; }
    public void setFeedbackType(FeedbackType feedbackType) { this.feedbackType = feedbackType; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public ComplianceStatus getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(ComplianceStatus complianceStatus) { this.complianceStatus = complianceStatus; }

    public Double getComplianceDetail() { return complianceDetail; }
    public void setComplianceDetail(Double complianceDetail) { this.complianceDetail = complianceDetail; }

    public Integer getRpe() { return rpe; }
    public void setRpe(Integer rpe) { this.rpe = rpe; }

    public String getSubjectiveOutcome() { return subjectiveOutcome; }
    public void setSubjectiveOutcome(String subjectiveOutcome) { this.subjectiveOutcome = subjectiveOutcome; }

    public Integer getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(Integer energyLevel) { this.energyLevel = energyLevel; }

    public Boolean getAdverseEvent() { return adverseEvent; }
    public void setAdverseEvent(Boolean adverseEvent) { this.adverseEvent = adverseEvent; }

    public String getAdverseEventDescription() { return adverseEventDescription; }
    public void setAdverseEventDescription(String adverseEventDescription) { this.adverseEventDescription = adverseEventDescription; }

    public Integer getAdverseEventSeverity() { return adverseEventSeverity; }
    public void setAdverseEventSeverity(Integer adverseEventSeverity) { this.adverseEventSeverity = adverseEventSeverity; }

    public String getContextNote() { return contextNote; }
    public void setContextNote(String contextNote) { this.contextNote = contextNote; }

    public String getEvidenceRef() { return evidenceRef; }
    public void setEvidenceRef(String evidenceRef) { this.evidenceRef = evidenceRef; }
}
