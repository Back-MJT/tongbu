package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.UserStage;

/**
 * 阶段行为配置 — 每个阶段的引擎行为参数.
 * Engine behavior configuration for a user stage.
 */
public class StageBehavior
{
    private UserStage stage;
    /** Multiplier applied to training difficulty (1.0 = baseline) */
    private double difficultyModifier;
    /** Tone/style of AI feedback */
    private String feedbackStyle;
    /** Type of incentive/achievement to offer */
    private String incentiveType;
    /** Key for the System Prompt template to use */
    private String promptTemplateKey;
    /** Human-readable stage behavior summary */
    private String description;

    public StageBehavior() {}

    public StageBehavior(UserStage stage, double difficultyModifier, String feedbackStyle,
                         String incentiveType, String promptTemplateKey, String description)
    {
        this.stage = stage;
        this.difficultyModifier = difficultyModifier;
        this.feedbackStyle = feedbackStyle;
        this.incentiveType = incentiveType;
        this.promptTemplateKey = promptTemplateKey;
        this.description = description;
    }

    // --- Getters & Setters ---

    public UserStage getStage() { return stage; }
    public void setStage(UserStage stage) { this.stage = stage; }

    public double getDifficultyModifier() { return difficultyModifier; }
    public void setDifficultyModifier(double v) { this.difficultyModifier = v; }

    public String getFeedbackStyle() { return feedbackStyle; }
    public void setFeedbackStyle(String v) { this.feedbackStyle = v; }

    public String getIncentiveType() { return incentiveType; }
    public void setIncentiveType(String v) { this.incentiveType = v; }

    public String getPromptTemplateKey() { return promptTemplateKey; }
    public void setPromptTemplateKey(String v) { this.promptTemplateKey = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}
