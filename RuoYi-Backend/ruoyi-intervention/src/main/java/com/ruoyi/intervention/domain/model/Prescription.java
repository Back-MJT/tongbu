package com.ruoyi.intervention.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ruoyi.intervention.domain.enums.InterventionType;

/**
 * 综合干预处方
 * Full intervention prescription — the primary output of the rule engine.
 *
 * Fields aligned with Python Prescription (src/models/prescription.py).
 */
public class Prescription
{
    private String prescriptionId;
    private String userId;
    private String interventionType;   // InterventionType code: "exercise", "sleep", etc.
    private LocalDateTime createdAt;
    private String status = "active";  // active / completed / cancelled
    private List<DailyExercise> exercises;           // for exercise prescriptions (7-day plan)
    private SleepRecommendation sleepPlan;            // for sleep prescriptions
    private List<AdjustmentRule> adjustmentRules = new ArrayList<>();
    private String notes;
    private int durationDays = 7;
    private LocalDate planDate;
    private double expectedScoreImprovement;  // predicted health score delta
    private String generatedBy;               // "rule_engine" or "claude"

    public Prescription() {}

    // --- Computed helpers ---

    /**
     * Total exercise duration in minutes for the day.
     */
    public int totalExerciseMinutes()
    {
        if (exercises == null) return 0;
        return exercises.stream().mapToInt(DailyExercise::getDurationMinutes).sum();
    }

    // --- Getters & Setters ---

    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getInterventionType() { return interventionType; }
    public void setInterventionType(String interventionType) { this.interventionType = interventionType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<DailyExercise> getExercises() { return exercises; }
    public void setExercises(List<DailyExercise> exercises) { this.exercises = exercises; }

    public SleepRecommendation getSleepPlan() { return sleepPlan; }
    public void setSleepPlan(SleepRecommendation sleepPlan) { this.sleepPlan = sleepPlan; }

    /** @deprecated Use {@link #getSleepPlan()} for alignment with Python model */
    @Deprecated
    public SleepRecommendation getSleepRecommendation() { return sleepPlan; }
    /** @deprecated Use {@link #setSleepPlan(SleepRecommendation)} */
    @Deprecated
    public void setSleepRecommendation(SleepRecommendation sleepRecommendation) { this.sleepPlan = sleepRecommendation; }

    public List<AdjustmentRule> getAdjustmentRules() { return adjustmentRules; }
    public void setAdjustmentRules(List<AdjustmentRule> adjustmentRules) { this.adjustmentRules = adjustmentRules; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }

    public double getExpectedScoreImprovement() { return expectedScoreImprovement; }
    public void setExpectedScoreImprovement(double expectedScoreImprovement) { this.expectedScoreImprovement = expectedScoreImprovement; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
}
