package com.xindong.llm.prompts;

/**
 * 提示词模板抽象基类 / Abstract base class for prompt templates.
 *
 * <p>Provides shared system prompt prefix and common formatting utilities.
 *
 * <p>Ticket: XIN-105
 */
public abstract class AbstractPromptTemplate implements PromptTemplate
{
    /** Shared system prompt prefix — role definition and safety constraints */
    private static final String SYSTEM_PROMPT_PREFIX =
        "你是昕动智能(HealthHub)的AI健身教练系统。你的任务是根据用户的健康数据和训练历史，"
        + "生成科学、个性化、安全的健身建议。\n\n"
        + "## 核心原则\n"
        + "1. **循证原则**：所有建议必须基于ACSM(美国运动医学会)2021指南和中国运动处方专家共识\n"
        + "2. **安全第一**：对有风险的用户优先考虑安全性，宁可保守不可激进\n"
        + "3. **个性化**：根据用户阶段、偏好和设备条件定制建议\n"
        + "4. **可执行**：建议必须具体到可以立即执行（指定运动类型、时长、强度、心率区间）\n"
        + "5. **中文输出**：所有面向用户的文本使用中文，技术字段使用英文\n\n"
        + "## 输出格式\n"
        + "你必须严格按照以下JSON格式输出，不要包含任何额外文本或markdown标记：\n";

    @Override
    public String systemPrompt()
    {
        return SYSTEM_PROMPT_PREFIX + expectedOutputSchema() + "\n\n" + scenarioInstructions();
    }

    /**
     * 场景特定指令 / Scenario-specific instructions appended after the shared prefix.
     */
    protected abstract String scenarioInstructions();

    /**
     * 格式化键值对为用户提示文本 / Format key-value pairs for user prompt.
     */
    protected String formatSection(String title, String content)
    {
        return "### " + title + "\n" + content + "\n\n";
    }

    /**
     * 格式化JSON数据块 / Format a JSON-like data block for the prompt.
     */
    protected String formatJsonBlock(String label, String json)
    {
        return "【" + label + "】\n" + json + "\n\n";
    }
}
