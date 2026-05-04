package com.ruoyi.intervention.domain.enums;

/**
 * 运动类型枚举
 * Types of exercise activities.
 */
public enum ExerciseType
{
    CYCLING("cycling", "骑行"),
    RUNNING("running", "跑步"),
    SWIMMING("swimming", "游泳"),
    WALKING("walking", "步行"),
    STRENGTH_TRAINING("strength_training", "力量训练"),
    STRETCHING("stretching", "拉伸"),
    HIIT("hiit", "高强度间歇训练"),
    YOGA("yoga", "瑜伽");

    private final String code;
    private final String labelZh;

    ExerciseType(String code, String labelZh)
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

    public static ExerciseType fromCode(String code)
    {
        for (ExerciseType e : values())
        {
            if (e.code.equals(code))
            {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown exercise type: " + code);
    }
}
