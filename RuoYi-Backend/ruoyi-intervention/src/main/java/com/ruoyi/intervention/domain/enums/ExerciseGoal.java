package com.ruoyi.intervention.domain.enums;

/**
 * 运动目标枚举 — 由RuleEngine根据健康画像确定
 * Exercise goal determined by the rule engine based on health profile.
 */
public enum ExerciseGoal
{
    WEIGHT_LOSS("weight_loss", "减重"),
    CARDIOVASCULAR("cardiovascular", "心肺改善"),
    MUSCULOSKELETAL("musculoskeletal", "骨骼肌肉"),
    GENERAL_FITNESS("general_fitness", "综合健身");

    private final String code;
    private final String labelZh;

    ExerciseGoal(String code, String labelZh)
    {
        this.code = code;
        this.labelZh = labelZh;
    }

    public String getCode() { return code; }
    public String getLabelZh() { return labelZh; }

    public static ExerciseGoal fromCode(String code)
    {
        for (ExerciseGoal g : values())
        {
            if (g.code.equals(code))
            {
                return g;
            }
        }
        throw new IllegalArgumentException("Unknown exercise goal: " + code);
    }
}
