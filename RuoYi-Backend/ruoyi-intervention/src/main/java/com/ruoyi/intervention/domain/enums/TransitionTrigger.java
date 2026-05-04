package com.ruoyi.intervention.domain.enums;

/**
 * 阶段转换触发器 — 导致用户阶段转换的事件类型.
 * Triggers that cause stage transitions.
 */
public enum TransitionTrigger
{
    FREQUENCY_THRESHOLD("frequency_threshold"),
    COMPLETION_RATE("completion_rate"),
    INACTIVITY_DAYS("inactivity_days"),
    INTENSITY_GROWTH("intensity_growth"),
    TENURE_WEEKS("tenure_weeks"),
    MANUAL_OVERRIDE("manual_override");

    private final String value;

    TransitionTrigger(String value)
    {
        this.value = value;
    }

    public String getValue() { return value; }

    public static TransitionTrigger fromValue(String v)
    {
        if (v == null) return MANUAL_OVERRIDE;
        for (TransitionTrigger t : values())
        {
            if (t.value.equalsIgnoreCase(v)) return t;
        }
        return MANUAL_OVERRIDE;
    }
}
