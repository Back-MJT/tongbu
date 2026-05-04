package com.ruoyi.intervention.domain.enums;

/**
 * 证据质量等级 (GRADE-based evidence quality level).
 */
public enum EvidenceLevel
{
    HIGH("high"),
    MODERATE("moderate"),
    LOW("low"),
    EXPERT("expert");

    private final String value;

    EvidenceLevel(String value)
    {
        this.value = value;
    }

    public String getValue() { return value; }

    public static EvidenceLevel fromValue(String v)
    {
        if (v == null) return EXPERT;
        for (EvidenceLevel e : values())
        {
            if (e.value.equalsIgnoreCase(v)) return e;
        }
        return EXPERT;
    }
}
