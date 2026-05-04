package com.xindong.llm.prompts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 提示词模板注册表 / Registry for prompt templates, keyed by scenario.
 *
 * <p>Central lookup for all available prompt templates. Auto-registers
 * all built-in templates on first access.
 *
 * <p>Usage:
 * <pre>
 *   PromptTemplate t = PromptTemplateRegistry.get(PromptScenario.DAILY_TASK);
 *   String system = t.systemPrompt();
 *   String user = t.userPrompt(input);
 * </pre>
 *
 * <p>Ticket: XIN-105
 */
public final class PromptTemplateRegistry
{
    private static final Map<PromptScenario, PromptTemplate> templates = new HashMap<>();

    static
    {
        registerBuiltIn(new InitialAssessmentPrompt());
        registerBuiltIn(new DailyTaskPrompt());
        registerBuiltIn(new ProgressReviewPrompt());
        registerBuiltIn(new PlanAdjustmentPrompt());
    }

    private PromptTemplateRegistry() {} // utility class

    /**
     * 注册模板 / Register a template (replaces existing for same scenario).
     */
    public static void register(PromptTemplate template)
    {
        templates.put(template.scenario(), template);
    }

    private static void registerBuiltIn(PromptTemplate template)
    {
        templates.put(template.scenario(), template);
    }

    /**
     * 获取模板 / Get template by scenario.
     *
     * @return the template, or null if no template registered for this scenario
     */
    public static PromptTemplate get(PromptScenario scenario)
    {
        return templates.get(scenario);
    }

    /**
     * 按场景code查找 / Get template by scenario code string.
     */
    public static PromptTemplate getByCode(String code)
    {
        PromptScenario scenario = PromptScenario.fromCode(code);
        return templates.get(scenario);
    }

    /**
     * 获取所有已注册模板 / Get all registered templates.
     */
    public static Map<PromptScenario, PromptTemplate> all()
    {
        return Collections.unmodifiableMap(templates);
    }

    /**
     * 检查场景是否有注册模板 / Check if a scenario has a registered template.
     */
    public static boolean has(PromptScenario scenario)
    {
        return templates.containsKey(scenario);
    }

    /**
     * 获取已注册模板数量 / Get count of registered templates.
     */
    public static int size()
    {
        return templates.size();
    }
}
