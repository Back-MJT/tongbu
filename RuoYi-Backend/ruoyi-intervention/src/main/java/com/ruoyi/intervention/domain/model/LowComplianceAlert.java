package com.ruoyi.intervention.domain.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.ruoyi.intervention.domain.enums.ComplianceLevel;

/**
 * 低依从性告警模型
 * Alert for a low-compliance user, suitable for injection into the
 * dashboard alert panel with alert_type=low_compliance.
 *
 * Evidence: Klasnja et al. 2015 (JITAIs)
 *
 * Migrated from: compliance_tracking.py LowComplianceAlert
 */
public class LowComplianceAlert
{
    /** 告警ID / Unique alert identifier */
    private String alertId;

    /** 用户标识 / User identifier */
    private String userId;

    /** 依从性等级 / Compliance level (should be LOW) */
    private ComplianceLevel complianceLevel;

    /** 连续未执行天数 / Consecutive days without intervention */
    private int daysWithoutIntervention;

    /** 最后干预日期 / ISO date string of last intervention */
    private String lastInterventionDate;

    /** 严重程度 / Always "warning" for low compliance */
    private String severity;

    /** 标题 / Alert title */
    private String title;

    /** 消息内容 / Alert message */
    private String message;

    /** 创建时间 / Timestamp of alert creation */
    private LocalDateTime createdAt;

    public LowComplianceAlert()
    {
        this.severity = "warning";
        this.createdAt = LocalDateTime.now();
    }

    // ========== Getters and Setters ==========

    public String getAlertId()
    {
        return alertId;
    }

    public void setAlertId(String alertId)
    {
        this.alertId = alertId;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public ComplianceLevel getComplianceLevel()
    {
        return complianceLevel;
    }

    public void setComplianceLevel(ComplianceLevel complianceLevel)
    {
        this.complianceLevel = complianceLevel;
    }

    public int getDaysWithoutIntervention()
    {
        return daysWithoutIntervention;
    }

    public void setDaysWithoutIntervention(int daysWithoutIntervention)
    {
        this.daysWithoutIntervention = daysWithoutIntervention;
    }

    public String getLastInterventionDate()
    {
        return lastInterventionDate;
    }

    public void setLastInterventionDate(String lastInterventionDate)
    {
        this.lastInterventionDate = lastInterventionDate;
    }

    public String getSeverity()
    {
        return severity;
    }

    public void setSeverity(String severity)
    {
        this.severity = severity;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public LocalDateTime getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt)
    {
        this.createdAt = createdAt;
    }

    /**
     * 将告警转换为API返回的字典格式
     * Convert this alert to a Map suitable for JSON API responses.
     *
     * @return Map with alert fields matching the Python to_alert_dict() output
     */
    public Map<String, Object> toAlertDict()
    {
        Map<String, Object> dict = new HashMap<>();
        dict.put("id", alertId);
        dict.put("user_id", userId);
        dict.put("alert_type", "low_compliance");
        dict.put("severity", severity);
        dict.put("title", title);
        dict.put("message", message);
        dict.put("created_at", createdAt.toString());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("compliance_level", complianceLevel != null ? complianceLevel.getCode() : null);
        metadata.put("days_without_intervention", daysWithoutIntervention);
        metadata.put("last_intervention_date", lastInterventionDate);
        dict.put("metadata", metadata);

        return dict;
    }
}
