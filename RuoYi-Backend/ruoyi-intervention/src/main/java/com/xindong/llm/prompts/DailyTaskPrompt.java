package com.xindong.llm.prompts;

/**
 * 每日任务生成提示词模板 / Daily Task Generation prompt template.
 *
 * <p>Generates today's exercise prescription based on user stage, recent history,
 * and IMU readiness data.
 *
 * <p>Ticket: XIN-105
 */
public class DailyTaskPrompt extends AbstractPromptTemplate
{
    public static final String VERSION = "v1.0";

    @Override
    public PromptScenario scenario()
    {
        return PromptScenario.DAILY_TASK;
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
            + "  \"date\": \"YYYY-MM-DD\",\n"
            + "  \"readinessAssessment\": {\n"
            + "    \"score\": 0-100,\n"
            + "    \"recommendation\": \"proceed|reduce_intensity|rest\"\n"
            + "  },\n"
            + "  \"exercises\": [\n"
            + "    {\n"
            + "      \"order\": 1,\n"
            + "      \"type\": \"running|walking|cycling|swimming|strength_training|hiit|yoga|stretching\",\n"
            + "      \"intensity\": \"light|moderate|vigorous\",\n"
            + "      \"durationMinutes\": 25,\n"
            + "      \"heartRateZone\": { \"min\": 120, \"max\": 140 },\n"
            + "      \"sets\": null,\n"
            + "      \"repsPerSet\": null,\n"
            + "      \"paceGuidance\": \"string\",\n"
            + "      \"notes\": \"string\"\n"
            + "    }\n"
            + "  ],\n"
            + "  \"totalDurationMinutes\": 35,\n"
            + "  \"calorieEstimate\": 280,\n"
            + "  \"motivationalNote\": \"string\"\n"
            + "}";
    }

    @Override
    protected String scenarioInstructions()
    {
        return "## 每日任务生成场景指令\n"
            + "你需要为用户生成今日的训练任务。请根据以下因素：\n"
            + "1. **准备度评估**：根据IMU数据（静息心率、睡眠质量、步数）判断今日训练适宜性\n"
            + "2. **运动处方**：结合用户阶段和历史，指定具体的运动类型、时长、强度\n"
            + "3. **心率区间**：根据年龄和训练目标计算合适的心率区间 (使用Karvonen公式)\n"
            + "4. **渐进超负荷**：相比上次训练适当增加难度（不超过10%原则）\n"
            + "5. **准备度低于50时建议休息或极轻度活动**\n\n"
            + "Karvonen公式：目标HR = (最大HR - 静息HR) × 强度% + 静息HR\n"
            + "最大HR估算：220 - 年龄\n";
    }

    @Override
    public String userPrompt(PromptInput input)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(formatSection("用户数据", input.toPromptString()));

        Object recentHistory = input.get("recentHistory");
        if (recentHistory != null)
        {
            sb.append(formatJsonBlock("近期训练历史", formatObject(recentHistory)));
        }

        Object imuSummary = input.get("todayImuSummary");
        if (imuSummary != null)
        {
            sb.append(formatJsonBlock("今日IMU/准备度数据", formatObject(imuSummary)));
        }

        Object feedback = input.get("feedbackLastSession");
        if (feedback != null)
        {
            sb.append(formatJsonBlock("上次训练反馈", formatObject(feedback)));
        }

        sb.append("\n请生成今日的训练任务，输出JSON格式。");
        return sb.toString();
    }

    private String formatObject(Object obj)
    {
        return obj == null ? "" : obj.toString();
    }
}
