package com.xindong.llm.prompts;

/**
 * 进展回顾提示词模板 / Progress Review prompt template.
 *
 * <p>Generates periodic progress reports with score trends, compliance analysis,
 * and next-period recommendations.
 *
 * <p>Ticket: XIN-105
 */
public class ProgressReviewPrompt extends AbstractPromptTemplate
{
    public static final String VERSION = "v1.0";

    @Override
    public PromptScenario scenario()
    {
        return PromptScenario.PROGRESS_REVIEW;
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
            + "  \"summary\": {\n"
            + "    \"overallTrend\": \"improving|stable|declining\",\n"
            + "    \"scoreDelta\": 7,\n"
            + "    \"gradeLetter\": \"A|B+|B|C+|C|D|F\"\n"
            + "  },\n"
            + "  \"highlights\": [\"string\"],\n"
            + "  \"areasForImprovement\": [\"string\"],\n"
            + "  \"stageTransition\": {\n"
            + "    \"recommended\": false,\n"
            + "    \"targetStage\": null,\n"
            + "    \"reasoning\": \"string\"\n"
            + "  },\n"
            + "  \"nextPeriodRecommendations\": [\"string\"],\n"
            + "  \"encouragementMessage\": \"string\"\n"
            + "}";
    }

    @Override
    protected String scenarioInstructions()
    {
        return "## 进展回顾场景指令\n"
            + "你需要为用户生成一份训练进展回顾报告。请：\n"
            + "1. **趋势判断**：分析健康分数变化趋势（改善/稳定/下降）\n"
            + "2. **亮点识别**：指出用户表现突出的维度\n"
            + "3. **改进方向**：识别需要加强的方面\n"
            + "4. **阶段转换评估**：判断是否需要调整用户阶段\n"
            + "   - beginner → growth: 连续4周完成率>70%，频率稳定\n"
            + "   - growth → plateau: 7天+无训练或无强度增长\n"
            + "   - growth → advanced: 3个月+高频率自驱训练\n"
            + "   - plateau → growth: 恢复训练2周+\n"
            + "5. **下一周期建议**：具体的训练调整建议\n"
            + "6. **鼓励语**：正面、真诚、具体的鼓励\n";
    }

    @Override
    public String userPrompt(PromptInput input)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(formatSection("回顾数据", input.toPromptString()));

        Object trend = input.get("healthScoreTrend");
        if (trend != null)
        {
            sb.append(formatJsonBlock("健康分数趋势", formatObject(trend)));
        }

        Object compliance = input.get("compliance");
        if (compliance != null)
        {
            sb.append(formatJsonBlock("训练依从性", formatObject(compliance)));
        }

        sb.append("\n请生成进展回顾报告，输出JSON格式。");
        return sb.toString();
    }

    private String formatObject(Object obj)
    {
        return obj == null ? "" : obj.toString();
    }
}
