package com.ruoyi.intervention.domain.enums;

/**
 * 依从性等级枚举
 * Compliance level classification for intervention plan adherence.
 *
 * Evidence: Klasnja et al. 2015 (JITAIs), Nahum-Shani et al. 2018
 *
 * Levels:
 *   HIGH   (green)  — compliant, following plan closely
 *   MEDIUM (yellow) — minor lapses but acceptable
 *   LOW    (orange) — 3+ consecutive days without intervention, requires attention
 *   NONE   (gray)   — no active prescription or no compliance data
 *
 * Migrated from: compliance_tracking.py ComplianceLevel
 */
public enum ComplianceLevel
{
    HIGH("high", "高依从性"),
    MEDIUM("medium", "中等依从性"),
    LOW("low", "低依从性"),
    NONE("none", "无数据");

    private final String code;
    private final String labelZh;

    ComplianceLevel(String code, String labelZh)
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

    /**
     * Look up ComplianceLevel by its string code.
     *
     * @param code the string code (e.g. "high", "low")
     * @return matching ComplianceLevel
     * @throws IllegalArgumentException if no match found
     */
    public static ComplianceLevel fromCode(String code)
    {
        for (ComplianceLevel level : values())
        {
            if (level.code.equals(code))
            {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown compliance level: " + code);
    }
}
