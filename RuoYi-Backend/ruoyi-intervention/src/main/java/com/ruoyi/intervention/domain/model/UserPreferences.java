package com.ruoyi.intervention.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户偏好设置
 * User preferences for interventions.
 */
public class UserPreferences
{
    private String exerciseTime = "morning";  // morning/afternoon/evening
    private List<String> exerciseType = new ArrayList<>();
    private Map<String, String> sleepSchedule;

    public UserPreferences() {}

    // --- Getters & Setters ---

    public String getExerciseTime() { return exerciseTime; }
    public void setExerciseTime(String exerciseTime) { this.exerciseTime = exerciseTime; }

    public List<String> getExerciseType() { return exerciseType; }
    public void setExerciseType(List<String> exerciseType) { this.exerciseType = exerciseType; }

    public Map<String, String> getSleepSchedule() { return sleepSchedule; }
    public void setSleepSchedule(Map<String, String> sleepSchedule) { this.sleepSchedule = sleepSchedule; }
}
