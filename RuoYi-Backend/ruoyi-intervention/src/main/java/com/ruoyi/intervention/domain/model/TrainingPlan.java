package com.ruoyi.intervention.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 训练计划 (多周)
 * Multi-week training plan containing daily prescriptions.
 */
public class TrainingPlan
{
    private String planId;
    private String userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalWeeks;
    private List<Prescription> dailyPrescriptions = new ArrayList<>();
    private double baselineHealthScore;
    private double targetHealthScore;
    private String status;  // draft/active/completed/cancelled

    public TrainingPlan() {}

    /**
     * Get prescription for a specific date, or null if not found.
     */
    public Prescription getPrescriptionForDate(LocalDate date)
    {
        return dailyPrescriptions.stream()
                .filter(p -> date.equals(p.getPlanDate()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Average weekly exercise minutes across the plan.
     */
    public double averageWeeklyExerciseMinutes()
    {
        if (dailyPrescriptions.isEmpty()) return 0;
        double total = dailyPrescriptions.stream()
                .mapToInt(Prescription::totalExerciseMinutes)
                .sum();
        double weeks = Math.max(1, totalWeeks);
        return total / weeks;
    }

    // --- Getters & Setters ---

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public int getTotalWeeks() { return totalWeeks; }
    public void setTotalWeeks(int totalWeeks) { this.totalWeeks = totalWeeks; }

    public List<Prescription> getDailyPrescriptions() { return dailyPrescriptions; }
    public void setDailyPrescriptions(List<Prescription> dailyPrescriptions) { this.dailyPrescriptions = dailyPrescriptions; }

    public double getBaselineHealthScore() { return baselineHealthScore; }
    public void setBaselineHealthScore(double baselineHealthScore) { this.baselineHealthScore = baselineHealthScore; }

    public double getTargetHealthScore() { return targetHealthScore; }
    public void setTargetHealthScore(double targetHealthScore) { this.targetHealthScore = targetHealthScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /**
     * 训练会话 / A single training session within a plan.
     */
    public static class TrainingSession
    {
        private int day;
        private String status;  // PENDING, COMPLETED, SKIPPED, etc.
        private String exerciseType;
        private int durationMinutes;
        private String intensity;

        public TrainingSession() {}

        // --- Getters & Setters ---
        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getExerciseType() { return exerciseType; }
        public void setExerciseType(String exerciseType) { this.exerciseType = exerciseType; }

        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

        public String getIntensity() { return intensity; }
        public void setIntensity(String intensity) { this.intensity = intensity; }
    }
}
