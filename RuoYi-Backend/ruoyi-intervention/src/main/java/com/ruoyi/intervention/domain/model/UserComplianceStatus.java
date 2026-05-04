package com.ruoyi.intervention.domain.model;

import java.time.LocalDateTime;

import com.ruoyi.intervention.domain.enums.ComplianceLevel;

/**
 * 用户依从性状态模型
 * Compliance status for a single user, capturing their adherence to
 * prescribed intervention plans.
 *
 * Evidence: Klasnja et al. 2015 (JITAIs), Nahum-Shani et al. 2018
 *
 * Migrated from: compliance_tracking.py UserComplianceStatus
 */
public class UserComplianceStatus
{
    /** 用户标识 / User identifier */
    private String userId;

    /** 依从性等级 / HIGH, MEDIUM, LOW, or NONE */
    private ComplianceLevel complianceLevel;

    /** 连续未执行天数 / Consecutive days since last intervention execution */
    private int daysWithoutIntervention;

    /** 最后一次执行日期(ISO格式) / ISO date string of last recorded execution, null if never */
    private String lastInterventionDate;

    /** 活跃处方数量 / Number of active prescriptions for this user */
    private int activePrescriptionCount;

    /** 7日依从率 / Proportion of days with intervention in last 7 days (0.0-1.0), null if insufficient data */
    private Double complianceRate7d;

    /** 计算时间 / Timestamp of this computation */
    private LocalDateTime computedAt;

    /** 等级原因说明 / Human-readable explanation of the compliance level */
    private String levelReason;

    /** 证据参考 / Evidence reference for the classification logic */
    private String evidenceRef;

    public UserComplianceStatus()
    {
        this.computedAt = LocalDateTime.now();
        this.evidenceRef = "klasnja_2015_jitai";
        this.daysWithoutIntervention = 0;
        this.activePrescriptionCount = 0;
    }

    // ========== Getters and Setters ==========

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

    public int getActivePrescriptionCount()
    {
        return activePrescriptionCount;
    }

    public void setActivePrescriptionCount(int activePrescriptionCount)
    {
        this.activePrescriptionCount = activePrescriptionCount;
    }

    public Double getComplianceRate7d()
    {
        return complianceRate7d;
    }

    public void setComplianceRate7d(Double complianceRate7d)
    {
        this.complianceRate7d = complianceRate7d;
    }

    public LocalDateTime getComputedAt()
    {
        return computedAt;
    }

    public void setComputedAt(LocalDateTime computedAt)
    {
        this.computedAt = computedAt;
    }

    public String getLevelReason()
    {
        return levelReason;
    }

    public void setLevelReason(String levelReason)
    {
        this.levelReason = levelReason;
    }

    public String getEvidenceRef()
    {
        return evidenceRef;
    }

    public void setEvidenceRef(String evidenceRef)
    {
        this.evidenceRef = evidenceRef;
    }
}
