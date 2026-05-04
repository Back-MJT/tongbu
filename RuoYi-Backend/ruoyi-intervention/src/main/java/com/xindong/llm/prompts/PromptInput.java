package com.xindong.llm.prompts;

import java.util.HashMap;
import java.util.Map;

/**
 * 提示词输入数据包装 / Input data wrapper for prompt template rendering.
 *
 * <p>Carries structured parameters that get serialized into the user prompt.
 * Each scenario uses different fields from this map.
 *
 * <p>Ticket: XIN-105
 */
public class PromptInput
{
    private final PromptScenario scenario;
    private final String userId;
    private final Map<String, Object> params;

    public PromptInput(PromptScenario scenario, String userId)
    {
        this.scenario = scenario;
        this.userId = userId;
        this.params = new HashMap<>();
    }

    public PromptInput(PromptScenario scenario, String userId, Map<String, Object> params)
    {
        this.scenario = scenario;
        this.userId = userId;
        this.params = params != null ? new HashMap<>(params) : new HashMap<>();
    }

    // ========== Fluent Builder ==========

    public PromptInput put(String key, Object value)
    {
        params.put(key, value);
        return this;
    }

    public PromptInput putAll(Map<String, Object> entries)
    {
        if (entries != null) params.putAll(entries);
        return this;
    }

    // ========== Convenience Builders for Common Fields ==========

    /** Set user stage (beginner/growth/plateau/advanced) */
    public PromptInput userStage(String stage)
    {
        params.put("userStage", stage);
        return this;
    }

    /** Set health profile data */
    public PromptInput healthProfile(Map<String, Object> profile)
    {
        params.put("healthProfile", profile);
        return this;
    }

    /** Set recent training history (list of session maps) */
    public PromptInput recentHistory(java.util.List<Map<String, Object>> history)
    {
        params.put("recentHistory", history);
        return this;
    }

    /** Set today's IMU/readiness summary */
    public PromptInput imuSummary(Map<String, Object> imu)
    {
        params.put("todayImuSummary", imu);
        return this;
    }

    /** Set compliance data for review */
    public PromptInput compliance(Map<String, Object> compliance)
    {
        params.put("compliance", compliance);
        return this;
    }

    /** Set health score trend for review */
    public PromptInput healthScoreTrend(Map<String, Object> trend)
    {
        params.put("healthScoreTrend", trend);
        return this;
    }

    /** Set current plan data for adjustment */
    public PromptInput currentPlan(Map<String, Object> plan)
    {
        params.put("currentPlan", plan);
        return this;
    }

    /** Set adjustment trigger reason */
    public PromptInput adjustmentTrigger(Map<String, Object> trigger)
    {
        params.put("adjustmentTrigger", trigger);
        return this;
    }

    // ========== Getters ==========

    public PromptScenario getScenario() { return scenario; }
    public String getUserId() { return userId; }

    @SuppressWarnings("unchecked")
    public <T> T get(String key)
    {
        return (T) params.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue)
    {
        Object val = params.get(key);
        return val != null ? (T) val : defaultValue;
    }

    public Map<String, Object> getParams()
    {
        return new HashMap<>(params);
    }

    /**
     * Convert to a flat string representation for prompt injection.
     * Produces a human-readable key=value format.
     */
    public String toPromptString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("用户ID: ").append(userId).append("\n");
        sb.append("场景: ").append(scenario.getLabelZh()).append("\n");
        appendMap(sb, "参数", params, 0);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendMap(StringBuilder sb, String prefix, Map<String, Object> map, int indent)
    {
        String pad = "  ".repeat(indent);
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            Object val = entry.getValue();
            if (val instanceof Map)
            {
                sb.append(pad).append(prefix).append(".").append(entry.getKey()).append(":\n");
                appendMap(sb, prefix + "." + entry.getKey(), (Map<String, Object>) val, indent + 1);
            }
            else if (val instanceof java.util.List)
            {
                sb.append(pad).append(entry.getKey()).append(": ").append(val).append("\n");
            }
            else
            {
                sb.append(pad).append(entry.getKey()).append(": ").append(val).append("\n");
            }
        }
    }
}
