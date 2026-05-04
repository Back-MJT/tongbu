package com.ruoyi.intervention.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 个体健康画像
 * Individual health profile — the core data structure for intervention generation.
 *
 * Reference: ACSM 2021 Ch.2 risk stratification, Ch.4 fitness assessment
 */
public class HealthProfile
{
    private String userId;
    private int age;
    private String gender;  // male/female/other
    private List<String> healthGoals = new ArrayList<>();
    private BaselineScores baselineScores;
    private RiskFactors riskFactors;
    private UserPreferences preferences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public HealthProfile() {}

    public HealthProfile(String userId, int age, String gender)
    {
        this.userId = userId;
        this.age = age;
        this.gender = gender;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate overall health score from baseline scores.
     * Returns 50 (neutral) if no baseline scores available.
     */
    public int calculateOverallScore()
    {
        if (baselineScores == null)
        {
            return 50;
        }
        return baselineScores.average();
    }

    // --- Getters & Setters ---

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public List<String> getHealthGoals() { return healthGoals; }
    public void setHealthGoals(List<String> healthGoals) { this.healthGoals = healthGoals; }

    public BaselineScores getBaselineScores() { return baselineScores; }
    public void setBaselineScores(BaselineScores baselineScores) { this.baselineScores = baselineScores; }

    public RiskFactors getRiskFactors() { return riskFactors; }
    public void setRiskFactors(RiskFactors riskFactors) { this.riskFactors = riskFactors; }

    public UserPreferences getPreferences() { return preferences; }
    public void setPreferences(UserPreferences preferences) { this.preferences = preferences; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
