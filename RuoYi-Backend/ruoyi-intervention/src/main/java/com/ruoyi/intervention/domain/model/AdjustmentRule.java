package com.ruoyi.intervention.domain.model;

/**
 * 动态调整规则
 * Adjustment rule for intervention modification based on feedback signals.
 */
public class AdjustmentRule
{
    private String condition;
    private String action;

    public AdjustmentRule() {}

    public AdjustmentRule(String condition, String action)
    {
        this.condition = condition;
        this.action = action;
    }

    // --- Getters & Setters ---

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
