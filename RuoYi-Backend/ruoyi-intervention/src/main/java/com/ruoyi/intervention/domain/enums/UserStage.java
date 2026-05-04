package com.ruoyi.intervention.domain.enums;

/**
 * 用户生命周期阶段 (User lifecycle stages in the training platform).
 *
 * Stages (per Board v1.1 Section 2.3):
 *   - BEGINNER  (初学期): 0-4 weeks, freq < 3/week, completion < 60%
 *   - GROWTH    (成长期): 1-3 months, freq stable, completion > 70%
 *   - PLATEAU   (停滞期): 7+ days inactivity, or no intensity growth
 *   - ADVANCED  (进阶期): 3+ months, high frequency, self-driven intensity increases
 *
 * Evidence base:
 *   - Prochaska & DiClemente 1983: Stages of Change (Transtheoretical Model)
 *   - Fogg 2020: "Tiny Habits" behavior model
 */
public enum UserStage
{
    BEGINNER("beginner", "初学期"),
    GROWTH("growth", "成长期"),
    PLATEAU("plateau", "停滞期"),
    ADVANCED("advanced", "进阶期");

    private final String value;
    private final String labelZh;

    UserStage(String value, String labelZh)
    {
        this.value = value;
        this.labelZh = labelZh;
    }

    public String getValue() { return value; }
    public String getLabelZh() { return labelZh; }

    /**
     * Parse from string value (case-insensitive).
     */
    public static UserStage fromValue(String v)
    {
        if (v == null) return BEGINNER;
        for (UserStage s : values())
        {
            if (s.value.equalsIgnoreCase(v)) return s;
        }
        return BEGINNER;
    }
}
