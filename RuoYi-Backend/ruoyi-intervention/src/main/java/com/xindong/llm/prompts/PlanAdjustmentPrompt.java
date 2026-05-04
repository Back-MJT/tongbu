package com.xindong.llm.prompts;

/**
 * 计划调整提示词模板 / Plan Adjustment prompt template.
 *
 * <p>Adjusts training plans when compliance drops, plateau is detected,
 * or user provides feedback requiring plan changes.
 *
 * <p>Ticket: XIN-105
 */
public class PlanAdjustmentPrompt extends AbstractPromptTemplate
{
    public static final String VERSION = "v1.0";

    @Override
    public PromptScenario scenario()
    {
        return PromptScenario.PLAN_ADJUSTMENT;
    }

    @Override
    public String version()
    {
        return VERSION;
    }

    @Override
    public String expectedOutputSchema()
    {
        return "{\n"
            + "  \"adjustmentType\": \"reduce_load|increase_variety|reschedule|change_intensity\",\n"
            + "  \"reasoning\": \"string\",\n"
            + "  \"adjustedPlan\": {\n"
            + "    \"weeklyFrequency\": 3,\n"
            + "    \"sessionDurationMinutes\": 20,\n"
            + "    \"intensity\": \"light|moderate|vigorous\",\n"
            + "    \"exerciseTypes\": [\"walking\", \"yoga\"]\n"
            + "  },\n"
            + "  \"transitionStrategy\": {\n"
            + "    \"steps\": [\"string\"],\n"
            + "    \"targetWeek\": 4\n"
            + "  },\n"
            + "  \"newAdjustmentRules\": [\n"
            + "    { \"trigger\": \"string\", \"action\": \"string\" }\n"
            + "  ],\n"
            + "  \"expectedComplianceImprovement\": 0.0-1.0\n"
            + "}";
    }

    @Override
    protected String scenarioInstructions()
    {
        return "## 计划调整场景指令\n"
            + "你需要根据用户的训练表现调整训练计划。调整策略：\n"
            + "1. **依从性低 (<50%)**：降频+降时长+换用户喜欢的运动类型\n"
            + "   - 先降低门槛让用户重新建立习惯（Fogg行为模型：让行为更容易）\n"
            + "2. **停滞期**：增加训练多样性，引入新的运动类型\n"
            + "   - 打破身体适应性 plateau\n"
            + "3. **用户反馈困难**：调整强度或时间安排\n"
            + "4. **过渡策略**：提供分阶段的恢复/调整计划\n"
            + "5. **自适应性规则**：设定自动触发条件，减少人工干预\n\n"
            + "注意：调整后的计划必须比当前更容易执行，而不是更难。\n";
    }

    @Override
    public String userPrompt(PromptInput input)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(formatSection("调整数据", input.toPromptString()));

        Object currentPlan = input.get("currentPlan");
        if (currentPlan != null)
        {
            sb.append(formatJsonBlock("当前训练计划", formatObject(currentPlan)));
        }

        Object trigger = input.get("adjustmentTrigger");
        if (trigger != null)
        {
            sb.append(formatJsonBlock("调整触发原因", formatObject(trigger)));
        }

        Object failureAnalysis = input.get("failureAnalysis");
        if (failureAnalysis != null)
        {
            sb.append(formatJsonBlock("失败原因分析", formatObject(failureAnalysis)));
        }

        Object prefs = input.get("userPreferences");
        if (prefs != null)
        {
            sb.append(formatJsonBlock("用户偏好", formatObject(prefs)));
        }

        sb.append("\n请生成调整后的训练计划，输出JSON格式。");
        return sb.toString();
    }

    private String formatObject(Object obj)
    {
        return obj == null ? "" : obj.toString();
    }
}
