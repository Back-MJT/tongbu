package com.ruoyi.intervention.domain.enums;

/**
 * 睡眠目标枚举 — 由RuleEngine根据健康画像确定
 * Sleep goal determined by the rule engine based on health profile.
 */
public enum SleepGoal
{
    POOR_SLEEP("poor_sleep", "改善睡眠"),
    IRREGULAR_SLEEP("irregular_sleep", "规律作息"),
    STRESS_SLEEP("stress_sleep", "减压助眠");

    private final String code;
    private final String labelZh;

    SleepGoal(String code, String labelZh)
    {
        this.code = code;
        this.labelZh = labelZh;
    }

    public String getCode() { return code; }
    public String getLabelZh() { return labelZh; }

    public static SleepGoal fromCode(String code)
    {
        for (SleepGoal g : values())
        {
            if (g.code.equals(code))
            {
                return g;
            }
        }
        throw new IllegalArgumentException("Unknown sleep goal: " + code);
    }
}
