package com.xindong.llm.prompts;

/**
 * 提示词模板接口 / Prompt template interface for callClaude() scenarios.
 *
 * <p>Each scenario (initial assessment, daily task, etc.) implements this interface
 * to provide the system prompt, user prompt rendering, and expected output schema.
 *
 * <p>Ticket: XIN-105
 */
public interface PromptTemplate
{
    /**
     * 此模板对应的场景 / The scenario this template handles.
     */
    PromptScenario scenario();

    /**
     * 模板版本号 / Version string for prompt versioning and cache invalidation.
     * Format: "v1.0", "v1.1", etc.
     */
    String version();

    /**
     * 系统提示词 / System prompt — the fixed instruction set for the LLM.
     * This defines the AI's role, constraints, and output format requirements.
     */
    String systemPrompt();

    /**
     * 渲染用户提示词 / Render the user prompt from structured input data.
     *
     * @param input structured input parameters
     * @return the user prompt string to send to the LLM
     */
    String userPrompt(PromptInput input);

    /**
     * 期望输出的JSON Schema描述 / Expected output JSON schema description.
     * Included in the system prompt to constrain LLM output format.
     */
    String expectedOutputSchema();
}
