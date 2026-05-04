package com.xindong.llm.prompts;

/**
 * 初始评估提示词模板 / Initial Assessment prompt template.
 *
 * <p>For new users or re-assessment. Produces initial stage assignment,
 * training recommendations, and first-week plan.
 *
 * <p>Ticket: XIN-105
 */
public class InitialAssessmentPrompt extends AbstractPromptTemplate
{
    public static final String VERSION = "v1.0";

    @Override
    public PromptScenario scenario()
    {
        return PromptScenario.INITIAL_ASSESSMENT;
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
            + "  \"stageAssessment\": {\n"
            + "    \"recommendedStage\": \"beginner|growth|plateau|advanced\",\n"
            + "    \"confidence\": 0.0-1.0,\n"
            + "    \"reasoning\": \"string\"\n"
            + "  },\n"
            + "  \"trainingRecommendation\": {\n"
            + "    \"weeklyFrequency\": 3,\n"
            + "    \"sessionDurationMinutes\": 30,\n"
            + "    \"intensity\": \"light|moderate|vigorous\",\n"
            + "    \"exerciseTypes\": [\"walking\", \"cycling\"],\n"
            + "    \"targetHeartRateZone\": { \"min\": 100, \"max\": 120 },\n"
            + "    \"warmupMinutes\": 5,\n"
            + "    \"cooldownMinutes\": 5\n"
            + "  },\n"
            + "  \"precautions\": [\"string\"],\n"
            + "  \"firstWeekPlan\": {\n"
            + "    \"days\": [\n"
            + "      {\n"
            + "        \"day\": 1,\n"
            + "        \"exercises\": [\n"
            + "          { \"type\": \"walking\", \"duration\": 20, \"intensity\": \"light\" }\n"
            + "        ]\n"
            + "      }\n"
            + "    ]\n"
            + "  },\n"
            + "  \"evidenceBasis\": \"string\"\n"
            + "}";
    }

    @Override
    protected String scenarioInstructions()
    {
        return "## 初始评估场景指令\n"
            + "你正在为新用户进行首次健身评估。请根据提供的健康画像信息：\n"
            + "1. 评估用户当前体能水平，推荐合适的阶段(beginner/growth/plateau/advanced)\n"
            + "2. 制定安全的起始训练方案，优先考虑低强度、短时长\n"
            + "3. 识别风险因素，提供必要的注意事项\n"
            + "4. 生成首周详细训练计划\n"
            + "5. 注明建议的循证依据\n\n"
            + "注意：对于有心血管或代谢风险的用户，必须建议先进行医学评估。\n";
    }

    @Override
    public String userPrompt(PromptInput input)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(formatSection("用户健康画像", input.toPromptString()));

        // Add structured data sections
        Object healthProfile = input.get("healthProfile");
        if (healthProfile != null)
        {
            sb.append(formatJsonBlock("健康画像详情", formatObject(healthProfile)));
        }

        sb.append("\n请根据以上信息，对该用户进行初始评估，输出JSON格式的评估结果。");
        return sb.toString();
    }

    private String formatObject(Object obj)
    {
        if (obj == null) return "";
        return obj.toString();
    }
}
