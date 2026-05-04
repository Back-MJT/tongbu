package com.ruoyi.intervention.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 执行依从性记录
 * Tracks whether a user completed their prescribed intervention.
 */
public class ComplianceRecord
{
    private String recordId;
    private String userId;
    private LocalDate planDate;
    private String prescriptionId;

    // Exercise compliance
    private double exerciseCompletionRate;   // 0.0 – 1.0
    private int actualExerciseMinutes;
    private int prescribedExerciseMinutes;

    // Sleep compliance
    private double sleepDeviationHours;      // |actual - prescribed| in hours
    private double actualSleepHours;
    private double prescribedSleepHours;

    // Overall
    private double complianceScore;          // weighted composite 0 – 100
    private String feedback;

    private LocalDateTime recordedAt;

    public ComplianceRecord() {}

    /**
     * Calculate exercise completion rate from actual vs prescribed.
     */
    public static double calcCompletionRate(int actual, int prescribed)
    {
        if (prescribed <= 0) return 1.0;
        return Math.min(1.0, (double) actual / prescribed);
    }

    // --- Getters & Setters ---

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }

    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }

    public double getExerciseCompletionRate() { return exerciseCompletionRate; }
    public void setExerciseCompletionRate(double exerciseCompletionRate) { this.exerciseCompletionRate = exerciseCompletionRate; }

    public int getActualExerciseMinutes() { return actualExerciseMinutes; }
    public void setActualExerciseMinutes(int actualExerciseMinutes) { this.actualExerciseMinutes = actualExerciseMinutes; }

    public int getPrescribedExerciseMinutes() { return prescribedExerciseMinutes; }
    public void setPrescribedExerciseMinutes(int prescribedExerciseMinutes) { this.prescribedExerciseMinutes = prescribedExerciseMinutes; }

    public double getSleepDeviationHours() { return sleepDeviationHours; }
    public void setSleepDeviationHours(double sleepDeviationHours) { this.sleepDeviationHours = sleepDeviationHours; }

    public double getActualSleepHours() { return actualSleepHours; }
    public void setActualSleepHours(double actualSleepHours) { this.actualSleepHours = actualSleepHours; }

    public double getPrescribedSleepHours() { return prescribedSleepHours; }
    public void setPrescribedSleepHours(double prescribedSleepHours) { this.prescribedSleepHours = prescribedSleepHours; }

    public double getComplianceScore() { return complianceScore; }
    public void setComplianceScore(double complianceScore) { this.complianceScore = complianceScore; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
