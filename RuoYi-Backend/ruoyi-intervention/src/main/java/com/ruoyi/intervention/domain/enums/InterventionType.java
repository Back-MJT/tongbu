package com.ruoyi.intervention.domain.enums;

/**
 * 干预类型枚举
 * Types of health interventions.
 */
public enum InterventionType
{
    EXERCISE("exercise", "运动干预"),
    SLEEP("sleep", "睡眠干预"),
    NUTRITION("nutrition", "营养干预"),
    STRESS("stress", "压力干预");

    private final String code;
    private final String labelZh;

    InterventionType(String code, String labelZh)
    {
        this.code = code;
        this.labelZh = labelZh;
    }

    public String getCode()
    {
        return code;
    }

    public String getLabelZh()
    {
        return labelZh;
    }

    public static InterventionType fromCode(String code)
    {
        for (InterventionType t : values())
        {
            if (t.code.equals(code))
            {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown intervention type: " + code);
    }
}
