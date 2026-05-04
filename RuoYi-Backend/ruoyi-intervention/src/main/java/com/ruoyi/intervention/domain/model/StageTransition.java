package com.ruoyi.intervention.domain.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.ruoyi.intervention.domain.enums.TransitionTrigger;
import com.ruoyi.intervention.domain.enums.UserStage;

/**
 * 阶段转换记录 — 记录用户从一个阶段到另一个阶段的转换.
 * Record of a stage transition event.
 */
public class StageTransition
{
    private String userId;
    private UserStage fromStage;
    private UserStage toStage;
    private TransitionTrigger trigger;
    private LocalDateTime timestamp;
    private Map<String, Object> evidence = new HashMap<>();

    public StageTransition() {
        this.timestamp = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public UserStage getFromStage() { return fromStage; }
    public void setFromStage(UserStage fromStage) { this.fromStage = fromStage; }

    public UserStage getToStage() { return toStage; }
    public void setToStage(UserStage toStage) { this.toStage = toStage; }

    public TransitionTrigger getTrigger() { return trigger; }
    public void setTrigger(TransitionTrigger trigger) { this.trigger = trigger; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getEvidence() { return evidence; }
    public void setEvidence(Map<String, Object> evidence) { this.evidence = evidence; }
}
