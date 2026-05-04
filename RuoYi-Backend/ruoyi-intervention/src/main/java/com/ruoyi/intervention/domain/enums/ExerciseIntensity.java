package com.ruoyi.intervention.domain.enums;

/**
 * 运动强度等级
 * Aligned with ACSM 2021 Ch.7 intensity zones (%HRR).
 */
public enum ExerciseIntensity
{
    LIGHT("light", "低强度", 0.30, 0.49),
    MODERATE("moderate", "中等强度", 0.50, 0.69),
    VIGOROUS("vigorous", "高强度", 0.70, 0.89);

    private final String code;
    private final String labelZh;
    private final double hrrLower;
    private final double hrrUpper;

    ExerciseIntensity(String code, String labelZh, double hrrLower, double hrrUpper)
    {
        this.code = code;
        this.labelZh = labelZh;
        this.hrrLower = hrrLower;
        this.hrrUpper = hrrUpper;
    }

    public String getCode()
    {
        return code;
    }

    public String getLabelZh()
    {
        return labelZh;
    }

    public double getHrrLower()
    {
        return hrrLower;
    }

    public double getHrrUpper()
    {
        return hrrUpper;
    }

    public static ExerciseIntensity fromCode(String code)
    {
        for (ExerciseIntensity e : values())
        {
            if (e.code.equals(code))
            {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown exercise intensity: " + code);
    }

    /**
     * Determine intensity from %HRR value.
     * Reference: ACSM 2021 Ch.7
     */
    public static ExerciseIntensity fromHrrFraction(double hrrFraction)
    {
        if (hrrFraction < 0.50)
        {
            return LIGHT;
        }
        else if (hrrFraction < 0.70)
        {
            return MODERATE;
        }
        else
        {
            return VIGOROUS;
        }
    }
}
