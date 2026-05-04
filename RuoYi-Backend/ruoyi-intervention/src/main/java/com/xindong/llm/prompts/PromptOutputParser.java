package com.xindong.llm.prompts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * LLM输出JSON解析器 / Parses LLM JSON output into structured maps.
 *
 * <p>Handles the common case of extracting JSON from LLM responses that may
 * include markdown code fences or other formatting artifacts.
 *
 * <p>Ticket: XIN-105
 */
public class PromptOutputParser
{
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 解析LLM输出为Map / Parse LLM JSON output into a Map.
     *
     * <p>Handles:
     * <ul>
     *   <li>Pure JSON string</li>
     *   <li>JSON wrapped in ```json ... ``` markdown fences</li>
     *   <li>JSON with leading/trailing whitespace or text</li>
     * </ul>
     *
     * @param rawOutput the raw LLM response text
     * @return parsed Map, or empty map if parsing fails
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseJson(String rawOutput)
    {
        if (rawOutput == null || rawOutput.isBlank())
        {
            return new HashMap<>();
        }

        String json = extractJson(rawOutput);
        try
        {
            return mapper.readValue(json, Map.class);
        }
        catch (JsonProcessingException e)
        {
            // Try one more time with relaxed parsing
            return new HashMap<>();
        }
    }

    /**
     * 解析为数组 / Parse LLM JSON output into a List.
     */
    @SuppressWarnings("unchecked")
    public static List<Object> parseJsonArray(String rawOutput)
    {
        if (rawOutput == null || rawOutput.isBlank())
        {
            return new ArrayList<>();
        }

        String json = extractJson(rawOutput);
        try
        {
            return mapper.readValue(json, List.class);
        }
        catch (JsonProcessingException e)
        {
            return new ArrayList<>();
        }
    }

    /**
     * 安全提取嵌套字段 / Safely extract a nested field from parsed output.
     *
     * @param data the parsed output map
     * @param path dot-separated path, e.g. "stageAssessment.recommendedStage"
     * @return the value, or null if path not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T getField(Map<String, Object> data, String path)
    {
        if (data == null || path == null) return null;

        String[] parts = path.split("\\.");
        Object current = data;
        for (String part : parts)
        {
            if (current instanceof Map)
            {
                current = ((Map<String, Object>) current).get(part);
            }
            else
            {
                return null;
            }
        }
        return (T) current;
    }

    /**
     * 从LLM输出中提取JSON字符串 / Extract JSON string from LLM output.
     * Strips markdown fences and surrounding text.
     */
    static String extractJson(String raw)
    {
        String trimmed = raw.trim();

        // Strip markdown code fences
        if (trimmed.startsWith("```json"))
        {
            trimmed = trimmed.substring(7);
        }
        else if (trimmed.startsWith("```"))
        {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        // If starts with { or [, assume it's JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("["))
        {
            return trimmed;
        }

        // Try to find JSON object or array within the text
        int objStart = trimmed.indexOf('{');
        int arrStart = trimmed.indexOf('[');

        if (objStart < 0 && arrStart < 0)
        {
            return "{}"; // No JSON found
        }

        int start;
        char endChar;
        if (objStart >= 0 && (arrStart < 0 || objStart < arrStart))
        {
            start = objStart;
            endChar = '}';
        }
        else
        {
            start = arrStart;
            endChar = ']';
        }

        // Find matching closing bracket
        int depth = 0;
        boolean inString = false;
        char prev = 0;
        for (int i = start; i < trimmed.length(); i++)
        {
            char c = trimmed.charAt(i);
            if (c == '"' && prev != '\\')
            {
                inString = !inString;
            }
            if (!inString)
            {
                if (c == '{' || c == '[') depth++;
                if (c == '}' || c == ']') depth--;
                if (depth == 0)
                {
                    return trimmed.substring(start, i + 1);
                }
            }
            prev = c;
        }

        return trimmed.substring(start); // Return what we have
    }

    /**
     * 验证输出是否包含必需字段 / Validate that parsed output contains required fields.
     *
     * @param data parsed output
     * @param requiredFields list of required top-level field names
     * @return list of missing field names (empty if all present)
     */
    public static List<String> validateRequired(Map<String, Object> data, String... requiredFields)
    {
        List<String> missing = new ArrayList<>();
        for (String field : requiredFields)
        {
            if (!data.containsKey(field))
            {
                missing.add(field);
            }
        }
        return missing;
    }
}
