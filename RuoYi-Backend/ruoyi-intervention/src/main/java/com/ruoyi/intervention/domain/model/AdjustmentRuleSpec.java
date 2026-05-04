package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.AdjustmentDimension;
import com.ruoyi.intervention.domain.enums.AdjustmentDirection;
import com.ruoyi.intervention.domain.enums.AdjustmentTriggerType;

import java.util.Map;

/**
 * Definition of a single adjustment rule: trigger condition -> adjustment action.
 * Rules are authored by domain experts and stored in the preset rule library.
 *
 * Reference: ACSM 2021 Ch.4 progressive overload; Nahum-Shani 2018 JITAI design
 */
public class AdjustmentRuleSpec {
    private String ruleId;
    private AdjustmentDimension dimension;
    private AdjustmentTriggerType triggerType;
    private Map<String, Object> triggerConditions;
    private AdjustmentDirection direction;
    private double magnitude;
    private int priority;
    private int cooldownHours;
    private Map<String, Double> safetyBounds;
    private String rationale;
    private String evidenceRef;
    private boolean active;

    public AdjustmentRuleSpec() {}

    public AdjustmentRuleSpec(String ruleId, AdjustmentDimension dimension,
            AdjustmentTriggerType triggerType, Map<String, Object> triggerConditions,
            AdjustmentDirection direction, double magnitude, int priority,
            int cooldownHours, Map<String, Double> safetyBounds,
            String rationale, String evidenceRef, boolean active) {
        this.ruleId = ruleId;
        this.dimension = dimension;
        this.triggerType = triggerType;
        this.triggerConditions = triggerConditions;
        this.direction = direction;
        this.magnitude = magnitude;
        this.priority = priority;
        this.cooldownHours = cooldownHours;
        this.safetyBounds = safetyBounds;
        this.rationale = rationale;
        this.evidenceRef = evidenceRef;
        this.active = active;
    }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public AdjustmentDimension getDimension() { return dimension; }
    public void setDimension(AdjustmentDimension dimension) { this.dimension = dimension; }
    public AdjustmentTriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(AdjustmentTriggerType triggerType) { this.triggerType = triggerType; }
    public Map<String, Object> getTriggerConditions() { return triggerConditions; }
    public void setTriggerConditions(Map<String, Object> triggerConditions) { this.triggerConditions = triggerConditions; }
    public AdjustmentDirection getDirection() { return direction; }
    public void setDirection(AdjustmentDirection direction) { this.direction = direction; }
    public double getMagnitude() { return magnitude; }
    public void setMagnitude(double magnitude) { this.magnitude = magnitude; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getCooldownHours() { return cooldownHours; }
    public void setCooldownHours(int cooldownHours) { this.cooldownHours = cooldownHours; }
    public Map<String, Double> getSafetyBounds() { return safetyBounds; }
    public void setSafetyBounds(Map<String, Double> safetyBounds) { this.safetyBounds = safetyBounds; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public String getEvidenceRef() { return evidenceRef; }
    public void setEvidenceRef(String evidenceRef) { this.evidenceRef = evidenceRef; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
