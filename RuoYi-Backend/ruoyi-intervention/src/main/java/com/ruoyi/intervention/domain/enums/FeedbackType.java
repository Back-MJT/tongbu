package com.ruoyi.intervention.domain.enums;

/**
 * Categories of feedback collected from users.
 */
public enum FeedbackType
{
    COMPLIANCE("compliance"),
    SUBJECTIVE_EXERTION("exertion"),
    SUBJECTIVE_OUTCOME("outcome"),
    ADVERSE_EVENT("adverse_event"),
    PREFERENCE("preference");

    private final String value;

    FeedbackType(String value) { this.value = value; }
    public String getValue() { return value; }

    public static FeedbackType fromValue(String v)
    {
        if (v == null) return COMPLIANCE;
        for (FeedbackType e : values())
        {
            if (e.value.equalsIgnoreCase(v)) return e;
        }
        return COMPLIANCE;
    }
}
