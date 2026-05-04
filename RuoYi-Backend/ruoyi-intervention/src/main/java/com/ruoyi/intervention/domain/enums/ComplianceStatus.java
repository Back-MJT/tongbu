package com.ruoyi.intervention.domain.enums;

/**
 * Overall compliance with prescribed intervention.
 */
public enum ComplianceStatus
{
    FULL("full"),
    PARTIAL("partial"),
    NONE("none"),
    DECLINED("declined");

    private final String value;

    ComplianceStatus(String value) { this.value = value; }
    public String getValue() { return value; }

    public static ComplianceStatus fromValue(String v)
    {
        if (v == null) return NONE;
        for (ComplianceStatus e : values())
        {
            if (e.value.equalsIgnoreCase(v)) return e;
        }
        return NONE;
    }
}
