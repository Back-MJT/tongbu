package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.UserStage;

/**
 * 用户训练行为数据 — 用于阶段识别的输入模型.
 * Training behavior data for a user, used as input for stage identification.
 */
public class UserTrainingData
{
    private String userId;
    /** Weeks since first training session */
    private double tenureWeeks = 0.0;
    /** Average training sessions per week (trailing 4 weeks) */
    private double weeklyFrequency = 0.0;
    /** Ratio of completed sessions to planned sessions [0.0, 1.0] */
    private double completionRate = 0.0;
    /** Date of last training session */
    private java.time.LocalDateTime lastSessionDate;
    /** Intensity growth rate — slope over last 4 weeks */
    private double intensityTrend = 0.0;
    /** Total training sessions completed */
    private int totalSessions = 0;

    public UserTrainingData() {}

    // --- Getters & Setters ---

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getTenureWeeks() { return tenureWeeks; }
    public void setTenureWeeks(double tenureWeeks) { this.tenureWeeks = tenureWeeks; }

    public double getWeeklyFrequency() { return weeklyFrequency; }
    public void setWeeklyFrequency(double weeklyFrequency) { this.weeklyFrequency = weeklyFrequency; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public java.time.LocalDateTime getLastSessionDate() { return lastSessionDate; }
    public void setLastSessionDate(java.time.LocalDateTime lastSessionDate) { this.lastSessionDate = lastSessionDate; }

    public double getIntensityTrend() { return intensityTrend; }
    public void setIntensityTrend(double intensityTrend) { this.intensityTrend = intensityTrend; }

    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
}
