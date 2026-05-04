package com.ruoyi.intervention.domain.enums;

/**
 * Rules governing how feedback adjusts intervention parameters.
 */
public enum AdaptationRule
{
    INCREASE_INTENSITY("increase_intensity"),
    DECREASE_INTENSITY("decrease_intensity"),
    MAINTAIN("maintain"),
    ADD_REST("add_rest"),
    SWITCH_TYPE("switch_type"),
    REDUCE_FREQUENCY("reduce_frequency");

    private final String value;

    AdaptationRule(String value) { this.value = value; }
    public String getValue() { return value; }

    public static AdaptationRule fromValue(String v)
    {
        if (v == null) return MAINTAIN;
        for (AdaptationRule e : values())
        {
            if (e.value.equalsIgnoreCase(v)) return e;
        }
        return MAINTAIN;
    }
}
